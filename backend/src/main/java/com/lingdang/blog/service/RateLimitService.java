package com.lingdang.blog.service;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.lingdang.blog.config.RateLimitConfig;
import com.lingdang.blog.repository.AssistantLogRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayDeque;
import java.util.Deque;
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
    
    // IP 请求时间窗口缓存
    private final LoadingCache<String, Deque<Long>> ipRequestTimes;
    
    public RateLimitService() {
        // 创建 IP 请求时间窗口缓存（10分钟过期）
        this.ipRequestTimes = CacheBuilder.newBuilder()
            .expireAfterAccess(10, TimeUnit.MINUTES)
            .maximumSize(10000)
            .build(new CacheLoader<String, Deque<Long>>() {
                @Override
                public Deque<Long> load(String key) {
                    return new ArrayDeque<>();
                }
            });
    }
    
    /**
     * 检查是否允许请求
     */
    public boolean allowRequest(String clientIp) {
        try {
            int limitPerMinute = getLimitPerMinute();
            if (limitPerMinute <= 0) {
                return true;
            }

            Deque<Long> timestamps = ipRequestTimes.get(clientIp);
            long now = System.currentTimeMillis();
            boolean allowed;

            synchronized (timestamps) {
                long windowStart = now - 60_000L;
                while (!timestamps.isEmpty() && timestamps.peekFirst() < windowStart) {
                    timestamps.pollFirst();
                }

                allowed = timestamps.size() < limitPerMinute;
                if (allowed) {
                    timestamps.addLast(now);
                }
            }
            
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

    private int getLimitPerMinute() {
        int permitsPerHour = rateLimitConfig.getAssistant().getPermitsPerHour();
        if (permitsPerHour <= 0) {
            return 0;
        }
        return Math.max(1, (int) Math.round(permitsPerHour / 60.0));
    }
}
