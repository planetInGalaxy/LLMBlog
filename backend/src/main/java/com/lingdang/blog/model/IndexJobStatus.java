package com.lingdang.blog.model;

/**
 * 索引任务状态枚举
 */
public enum IndexJobStatus {
    /**
     * 等待执行
     */
    PENDING,
    
    /**
     * 执行中
     */
    RUNNING,
    
    /**
     * 成功
     */
    SUCCESS,
    
    /**
     * 失败
     */
    FAILED
}
