package com.lingdang.blog.model;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * 文章摘要生成任务（异步调用大模型生成 summary，一句话 25~40 字）。
 */
@Data
@Entity
@Table(name = "article_summary_jobs", indexes = {
    @Index(name = "idx_article_summary_jobs_article_id", columnList = "article_id"),
    @Index(name = "idx_article_summary_jobs_status", columnList = "status"),
    @Index(name = "idx_article_summary_jobs_created_at", columnList = "created_at")
})
public class ArticleSummaryJob {

    public enum Mode {
        /**
         * 仅当 summary 为空时才回填（避免覆盖人工摘要）
         */
        FILL_IF_EMPTY,
        /**
         * 用户显式要求重生成：允许覆盖现有 summary
         */
        REGENERATE
    }

    public enum Status {
        PENDING,
        RUNNING,
        SUCCESS,
        FAILED,
        CANCELED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "article_id", nullable = false)
    private Long articleId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Mode mode = Mode.FILL_IF_EMPTY;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status = Status.PENDING;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "retry_count", nullable = false)
    private Integer retryCount = 0;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
