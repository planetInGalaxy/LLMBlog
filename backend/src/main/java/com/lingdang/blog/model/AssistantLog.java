package com.lingdang.blog.model;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * 助手查询日志实体
 */
@Data
@Entity
@Table(name = "assistant_logs", indexes = {
    @Index(name = "idx_request_id", columnList = "request_id", unique = true),
    @Index(name = "idx_client_ip", columnList = "client_ip"),
    @Index(name = "idx_created_at", columnList = "created_at")
})
public class AssistantLog {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * 请求唯一标识符
     */
    @Column(name = "request_id", nullable = false, unique = true, length = 100)
    private String requestId;
    
    /**
     * 客户端 IP
     */
    @Column(name = "client_ip", length = 50)
    private String clientIp;
    
    /**
     * 用户问题
     */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String question;
    
    /**
     * 查询模式（ARTICLE_ONLY 等）
     */
    @Column(length = 50)
    private String mode;
    
    /**
     * 检索到的文章 ID 列表（逗号分隔）
     */
    @Column(name = "hit_article_ids", length = 500)
    private String hitArticleIds;
    
    /**
     * 引用数量
     */
    @Column(name = "citations_count")
    private Integer citationsCount;
    
    /**
     * LLM 模型名称
     */
    @Column(name = "llm_model", length = 100)
    private String llmModel;
    
    /**
     * Token 使用量（JSON 格式）
     */
    @Column(name = "token_usage", length = 500)
    private String tokenUsage;
    
    /**
     * 响应延迟（毫秒）
     */
    @Column(name = "latency_ms")
    private Integer latencyMs;
    
    /**
     * 是否成功
     */
    @Column(nullable = false)
    private Boolean success = true;
    
    /**
     * 错误信息
     */
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;
    
    /**
     * 创建时间
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
