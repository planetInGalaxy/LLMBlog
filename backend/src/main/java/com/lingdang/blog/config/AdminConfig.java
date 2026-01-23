package com.lingdang.blog.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 管理员配置类
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "admin")
public class AdminConfig {
    
    /**
     * 管理员用户名
     */
    private String username;
    
    /**
     * 管理员密码
     */
    private String password;
}
