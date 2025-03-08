package com.zeyang.login.service;

import com.zeyang.login.entity.User;
import com.zeyang.login.entity.UserStatus;
import com.zeyang.login.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.Optional;

@Service
public class UserService {
    
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    
    @Autowired
    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }
    
    public Optional<User> findByUsername(String username) {
        return Optional.ofNullable(userRepository.findByUsername(username));
    }
    
    @Transactional
    public User registerUser(String username, String password, String email, String phone) {
        // 检查用户名是否已存在
        if (userRepository.findByUsername(username) != null) {
            throw new RuntimeException("用户名已存在");
        }
        
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPhone(phone);
        user.setStatus(UserStatus.ACTIVE);
        user.setFailedAttempts(0);
        
        // 生成随机盐
        String salt = generateSalt();
        user.setSalt(salt);
        
        // 使用盐和密码生成哈希
        String passwordHash = hashPassword(password, salt);
        user.setPasswordHash(passwordHash);
        
        return userRepository.save(user);
    }
    
    public boolean verifyPassword(User user, String password) {
        String hashedPassword = hashPassword(password, user.getSalt());
        return hashedPassword.equals(user.getPasswordHash());
    }
    
    private String generateSalt() {
        SecureRandom random = new SecureRandom();
        byte[] salt = new byte[16];
        random.nextBytes(salt);
        return Base64.getEncoder().encodeToString(salt);
    }
    
    private String hashPassword(String password, String salt) {
        // 在实际应用中，应使用更安全的算法如PBKDF2、BCrypt等
        // 这里使用简单的SHA-256作为示例
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
            md.update(salt.getBytes());
            byte[] hashedPassword = md.digest(password.getBytes());
            return Base64.getEncoder().encodeToString(hashedPassword);
        } catch (Exception e) {
            throw new RuntimeException("密码加密失败", e);
        }
    }
}
