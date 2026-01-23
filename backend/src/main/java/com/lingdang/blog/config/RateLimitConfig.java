package com.lingdang.blog.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 限流配置类
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "rate-limit")
public class RateLimitConfig {
    
    /**
     * Assistant 限流配置
     */
    private AssistantRateLimit assistant = new AssistantRateLimit();
    
    @Data
    public static class AssistantRateLimit {
        /**
         * 每小时允许的请求数（默认 360 = 6次/分钟）
         */
        private int permitsPerHour = 360;
    }
}
