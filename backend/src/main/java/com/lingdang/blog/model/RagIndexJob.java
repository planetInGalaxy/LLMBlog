package com.lingdang.blog.model;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * RAG 索引任务实体
 */
@Data
@Entity
@Table(name = "rag_index_jobs", indexes = {
    @Index(name = "idx_article_status", columnList = "article_id,status"),
    @Index(name = "idx_status", columnList = "status")
})
public class RagIndexJob {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * 关联的文章 ID
     */
    @Column(name = "article_id", nullable = false)
    private Long articleId;
    
    /**
     * 任务状态
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private IndexJobStatus status = IndexJobStatus.PENDING;
    
    /**
     * 目标索引版本号
     */
    @Column(name = "target_index_version", nullable = false)
    private Integer targetIndexVersion;
    
    /**
     * 生成的 chunk 数量
     */
    @Column(name = "chunks_generated")
    private Integer chunksGenerated;
    
    /**
     * 已索引的 chunk 数量
     */
    @Column(name = "chunks_indexed")
    private Integer chunksIndexed;
    
    /**
     * 错误信息
     */
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;
    
    /**
     * 错误堆栈
     */
    @Column(name = "error_stack", columnDefinition = "TEXT")
    private String errorStack;
    
    /**
     * 重试次数
     */
    @Column(name = "retry_count", nullable = false)
    private Integer retryCount = 0;
    
    /**
     * 开始时间
     */
    @Column(name = "started_at")
    private LocalDateTime startedAt;
    
    /**
     * 完成时间
     */
    @Column(name = "completed_at")
    private LocalDateTime completedAt;
    
    /**
     * 创建时间
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    /**
     * 更新时间
     */
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
