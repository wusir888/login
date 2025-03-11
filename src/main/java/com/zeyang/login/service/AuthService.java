package com.zeyang.login.service;

import com.zeyang.login.entity.AuthAction;
import com.zeyang.login.entity.AuthLog;
import com.zeyang.login.entity.User;
import com.zeyang.login.entity.UserStatus;
import com.zeyang.login.repository.AuthLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class AuthService {
    
    private final UserService userService;
    private final AuthLogRepository authLogRepository;
    private final StringRedisTemplate redisTemplate;
    
    @Autowired
    public AuthService(UserService userService, AuthLogRepository authLogRepository, StringRedisTemplate redisTemplate) {
        this.userService = userService;
        this.authLogRepository = authLogRepository;
        this.redisTemplate = redisTemplate;
    }
    
    @Transactional
    public User login(String username, String password, HttpServletRequest request) {
        User user = userService.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("用户名或密码错误"));
        
        // 检查账户状态
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new RuntimeException("账户已被锁定或禁用");
        }
        
        // 检查账户是否被临时锁定
        if (user.getLockedUntil() != null && user.getLockedUntil().isAfter(LocalDateTime.now())) {
            throw new RuntimeException("账户已被临时锁定，请稍后再试");
        }
        
        // 验证密码
        boolean passwordValid = userService.verifyPassword(user, password);
        
        // 记录登录日志
        AuthLog authLog = new AuthLog();
        authLog.setUser(user);
        authLog.setIpAddress(getClientIp(request));
        authLog.setUserAgent(request.getHeader("User-Agent"));
        
        if (passwordValid) {
            // 登录成功
            authLog.setAction(AuthAction.LOGIN_SUCCESS);
            user.setFailedAttempts(0);
            user.setLockedUntil(null);
        } else {
            // 登录失败
            authLog.setAction(AuthAction.LOGIN_FAILURE);
            
            // 增加失败次数
            user.setFailedAttempts(user.getFailedAttempts() + 1);
            
            // 如果失败次数达到阈值，锁定账户
            if (user.getFailedAttempts() >= 5) {
                user.setLockedUntil(LocalDateTime.now().plusMinutes(30));
            }
            
            throw new RuntimeException("用户名或密码错误");
        }
        
        authLogRepository.save(authLog);
        return user;
    }
    
    @Transactional
    public void logout(User user, HttpServletRequest request) {
        AuthLog authLog = new AuthLog();
        authLog.setUser(user);
        authLog.setAction(AuthAction.LOGOUT);
        authLog.setIpAddress(getClientIp(request));
        authLog.setUserAgent(request.getHeader("User-Agent"));
        
        authLogRepository.save(authLog);
    }
    
    private String getClientIp(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader == null) {
            return request.getRemoteAddr();
        }
        return xfHeader.split(",")[0];
    }

    // 使用Redis存储会话信息
    public class RedisSessionManager {
        private final static String SESSION_PREFIX = "session:";
        private final static long SESSION_TIMEOUT = 30; // 分钟
        
        public void saveSession(String sessionId, User user) {
            String key = SESSION_PREFIX + sessionId;
            redisTemplate.opsForValue().set(key, JSON.toJSONString(user));
            redisTemplate.expire(key, SESSION_TIMEOUT, TimeUnit.MINUTES);
        }
        
        public User getSession(String sessionId) {
            String key = SESSION_PREFIX + sessionId;
            String userJson = redisTemplate.opsForValue().get(key);
            return userJson != null ? JSON.parseObject(userJson, User.class) : null;
        }
        
        public void removeSession(String sessionId) {
            redisTemplate.delete(SESSION_PREFIX + sessionId);
        }
    }

    // 使用Redis缓存登录令牌
    public class TokenManager {
        private final StringRedisTemplate redisTemplate;
        private final static String TOKEN_PREFIX = "token:";
        private final static long TOKEN_TIMEOUT = 24; // 小时
        
        public String createToken(User user) {
            String token = UUID.randomUUID().toString();
            String key = TOKEN_PREFIX + token;
            redisTemplate.opsForValue().set(key, user.getId().toString());
            redisTemplate.expire(key, TOKEN_TIMEOUT, TimeUnit.HOURS);
            return token;
        }
        
        public Long validateToken(String token) {
            String key = TOKEN_PREFIX + token;
            String userId = redisTemplate.opsForValue().get(key);
            return userId != null ? Long.valueOf(userId) : null;
        }
        
        public void refreshToken(String token) {
            String key = TOKEN_PREFIX + token;
            redisTemplate.expire(key, TOKEN_TIMEOUT, TimeUnit.HOURS);
        }
        
        public void invalidateToken(String token) {
            redisTemplate.delete(TOKEN_PREFIX + token);
        }
    }

    // 使用Redis实现登录失败计数和账户锁定
    public class LoginAttemptService {
        private final StringRedisTemplate redisTemplate;
        private final static String ATTEMPTS_PREFIX = "login_attempts:";
        private final static String LOCKOUT_PREFIX = "account_lock:";
        private final static int MAX_ATTEMPTS = 5;
        private final static long LOCK_TIME_MINUTES = 30;
        
        public void recordFailedAttempt(String username) {
            String attemptsKey = ATTEMPTS_PREFIX + username;
            redisTemplate.opsForValue().increment(attemptsKey);
            redisTemplate.expire(attemptsKey, 1, TimeUnit.DAYS);
            
            // 检查是否需要锁定账户
            String attempts = redisTemplate.opsForValue().get(attemptsKey);
            if (attempts != null && Integer.parseInt(attempts) >= MAX_ATTEMPTS) {
                lockAccount(username);
            }
        }
        
        private void lockAccount(String username) {
            String lockKey = LOCKOUT_PREFIX + username;
            redisTemplate.opsForValue().set(lockKey, "locked");
            redisTemplate.expire(lockKey, LOCK_TIME_MINUTES, TimeUnit.MINUTES);
        }
        
        public boolean isAccountLocked(String username) {
            return Boolean.TRUE.equals(redisTemplate.hasKey(LOCKOUT_PREFIX + username));
        }
        
        public void resetFailedAttempts(String username) {
            redisTemplate.delete(ATTEMPTS_PREFIX + username);
        }
    }

    // 使用Redis实现IP限流
    public class RateLimiter {
        private final StringRedisTemplate redisTemplate;
        private final static String IP_LIMIT_PREFIX = "ip_limit:";
        private final static int MAX_REQUESTS = 10;
        private final static long WINDOW_SECONDS = 60;
        
        public boolean allowRequest(String ip) {
            String key = IP_LIMIT_PREFIX + ip;
            Long count = redisTemplate.opsForValue().increment(key);
            
            // 第一次访问时设置过期时间
            if (count != null && count == 1) {
                redisTemplate.expire(key, WINDOW_SECONDS, TimeUnit.SECONDS);
            }
            
            return count != null && count <= MAX_REQUESTS;
        }
    }
} 