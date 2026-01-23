package com.lingdang.blog.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * JWT 配置类
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "jwt")
public class JwtConfig {
    
    /**
     * JWT 密钥
     */
    private String secret;
    
    /**
     * 过期时间（毫秒）
     */
    private Long expiration = 86400000L; // 24 小时
}
