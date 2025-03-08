package com.zeyang.login.controller;

import com.zeyang.login.dto.AuthResponse;
import com.zeyang.login.dto.LoginRequest;
import com.zeyang.login.dto.RegisterRequest;
import com.zeyang.login.entity.User;
import com.zeyang.login.service.AuthService;
import com.zeyang.login.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.validation.Valid;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    
    private final UserService userService;
    private final AuthService authService;
    
    @Autowired
    public AuthController(UserService userService, AuthService authService) {
        this.userService = userService;
        this.authService = authService;
    }
    
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        User user = userService.registerUser(
                request.getUsername(),
                request.getPassword(),
                request.getEmail(),
                request.getPhone()
        );
        
        return ResponseEntity.ok(new AuthResponse(
                null,
                user.getUsername(),
                "注册成功"
        ));
    }
    
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request, 
                                             HttpServletRequest servletRequest) {
        User user = authService.login(request.getUsername(), request.getPassword(), servletRequest);
        
        // 创建会话
        HttpSession session = servletRequest.getSession(true);
        session.setAttribute("USER_ID", user.getId());
        session.setAttribute("USERNAME", user.getUsername());
        
        return ResponseEntity.ok(new AuthResponse(
                session.getId(),
                user.getUsername(),
                "登录成功"
        ));
    }
    
    @PostMapping("/logout")
    public ResponseEntity<AuthResponse> logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            Long userId = (Long) session.getAttribute("USER_ID");
            String username = (String) session.getAttribute("USERNAME");
            
            if (userId != null) {
                User user = userService.findByUsername(username).orElse(null);
                if (user != null) {
                    authService.logout(user, request);
                }
            }
            
            session.invalidate();
        }
        
        return ResponseEntity.ok(new AuthResponse(
                null,
                null,
                "已成功退出登录"
        ));
    }
} 