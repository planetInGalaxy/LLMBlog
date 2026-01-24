package com.lingdang.blog.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * 异步任务配置
 * 
 * 使用线程池管理异步任务，避免 new Thread() 造成的资源浪费
 * 对于低配服务器，线程池可以有效控制并发数量
 */
@Configuration
@EnableAsync
public class AsyncConfig {
    
    /**
     * SSE 流式输出专用线程池
     * 
     * 特点：
     * - 核心线程数较小（适合低配服务器）
     * - 队列容量较大（允许排队等待）
     * - 空闲线程及时回收
     */
    @Bean(name = "sseTaskExecutor")
    public Executor sseTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);          // 核心线程数（低配服务器）
        executor.setMaxPoolSize(10);          // 最大线程数
        executor.setQueueCapacity(50);        // 队列容量
        executor.setKeepAliveSeconds(60);     // 空闲线程存活时间
        executor.setThreadNamePrefix("sse-");
        executor.setRejectedExecutionHandler((r, e) -> {
            // 队列满时，直接在调用者线程执行（降级处理）
            if (!e.isShutdown()) {
                r.run();
            }
        });
        executor.initialize();
        return executor;
    }
    
    /**
     * 索引任务专用线程池
     */
    @Bean(name = "indexTaskExecutor")
    public Executor indexTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(1);          // 索引任务串行执行
        executor.setMaxPoolSize(2);
        executor.setQueueCapacity(20);
        executor.setKeepAliveSeconds(120);
        executor.setThreadNamePrefix("index-");
        executor.initialize();
        return executor;
    }
}
