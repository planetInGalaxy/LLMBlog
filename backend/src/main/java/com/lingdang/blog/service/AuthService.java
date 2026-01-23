package com.lingdang.blog.service;

import com.lingdang.blog.config.AdminConfig;
import com.lingdang.blog.config.JwtConfig;
import com.lingdang.blog.dto.auth.LoginRequest;
import com.lingdang.blog.dto.auth.LoginResponse;
import com.lingdang.blog.util.JwtUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 认证服务
 */
@Slf4j
@Service
public class AuthService {
    
    @Autowired
    private AdminConfig adminConfig;
    
    @Autowired
    private JwtConfig jwtConfig;
    
    @Autowired
    private JwtUtil jwtUtil;
    
    /**
     * 管理员登录
     */
    public LoginResponse login(LoginRequest request) {
        // 验证用户名和密码
        if (!adminConfig.getUsername().equals(request.getUsername()) ||
            !adminConfig.getPassword().equals(request.getPassword())) {
            throw new RuntimeException("用户名或密码错误");
        }
        
        // 生成 Token
        String token = jwtUtil.generateToken(request.getUsername());
        
        log.info("管理员登录成功: {}", request.getUsername());
        
        return new LoginResponse(token, request.getUsername(), jwtConfig.getExpiration());
    }
    
    /**
     * 验证 Token
     */
    public boolean validateToken(String token) {
        return jwtUtil.validateToken(token);
    }
    
    /**
     * 从 Token 获取用户名
     */
    public String getUsernameFromToken(String token) {
        return jwtUtil.getUsernameFromToken(token);
    }
}
