package com.lingdang.blog.service;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.util.concurrent.RateLimiter;
import com.lingdang.blog.config.RateLimitConfig;
import com.lingdang.blog.repository.AssistantLogRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * 限流服务
 */
@Slf4j
@Service
public class RateLimitService {
    
    @Autowired
    private RateLimitConfig rateLimitConfig;
    
    @Autowired
    private AssistantLogRepository assistantLogRepository;
    
    // IP 限流器缓存
    private final LoadingCache<String, RateLimiter> ipRateLimiters;
    
    public RateLimitService() {
        // 创建 IP 限流器缓存（10分钟过期）
        this.ipRateLimiters = CacheBuilder.newBuilder()
            .expireAfterAccess(10, TimeUnit.MINUTES)
            .maximumSize(10000)
            .build(new CacheLoader<String, RateLimiter>() {
                @Override
                public RateLimiter load(String key) {
                    // 默认每小时30次
                    double permitsPerSecond = 30.0 / 3600.0;
                    return RateLimiter.create(permitsPerSecond);
                }
            });
    }
    
    /**
     * 检查是否允许请求
     */
    public boolean allowRequest(String clientIp) {
        try {
            // 使用 Guava RateLimiter
            RateLimiter limiter = ipRateLimiters.get(clientIp);
            boolean allowed = limiter.tryAcquire();
            
            if (!allowed) {
                log.warn("限流触发: ip={}", clientIp);
            }
            
            return allowed;
        } catch (ExecutionException e) {
            log.error("限流检查失败", e);
            return true; // 失败时允许请求
        }
    }
    
    /**
     * 获取 IP 在指定时间范围内的请求次数
     */
    public long getRequestCount(String clientIp, int hours) {
        LocalDateTime since = LocalDateTime.now().minusHours(hours);
        return assistantLogRepository.countByClientIpAndCreatedAtAfter(clientIp, since);
    }
    
    /**
     * 检查 IP 是否超过小时限制（数据库统计）
     */
    public boolean isRateLimited(String clientIp) {
        long count = getRequestCount(clientIp, 1);
        int limit = rateLimitConfig.getAssistant().getPermitsPerHour();
        return count >= limit;
    }
}
