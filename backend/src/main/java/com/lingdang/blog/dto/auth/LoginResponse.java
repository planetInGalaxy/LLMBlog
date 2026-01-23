package com.lingdang.blog.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 登录响应 DTO
 */
@Data
@AllArgsConstructor
public class LoginResponse {
    
    /**
     * JWT Token
     */
    private String token;
    
    /**
     * 用户名
     */
    private String username;
    
    /**
     * 过期时间（毫秒）
     */
    private Long expiresIn;
}
