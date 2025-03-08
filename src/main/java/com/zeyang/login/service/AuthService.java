package com.zeyang.login.service;

import com.zeyang.login.entity.AuthAction;
import com.zeyang.login.entity.AuthLog;
import com.zeyang.login.entity.User;
import com.zeyang.login.entity.UserStatus;
import com.zeyang.login.repository.AuthLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;

@Service
public class AuthService {
    
    private final UserService userService;
    private final AuthLogRepository authLogRepository;
    
    @Autowired
    public AuthService(UserService userService, AuthLogRepository authLogRepository) {
        this.userService = userService;
        this.authLogRepository = authLogRepository;
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
} 