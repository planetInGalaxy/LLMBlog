package com.lingdang.blog.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * 异步任务配置
 */
@Configuration
@EnableAsync
public class AsyncConfig {
    // 使用默认的异步执行器
}
