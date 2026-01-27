package com.lingdang.blog.model;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * 全量重建索引任务（异步）
 */
@Data
@Entity
@Table(name = "rag_reindex_jobs", indexes = {
    @Index(name = "idx_rag_reindex_created_at", columnList = "created_at"),
    @Index(name = "idx_rag_reindex_status", columnList = "status")
})
public class RagReindexJob {

    public enum Status {
        PENDING,
        RUNNING,
        SUCCESS,
        FAILED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status = Status.PENDING;

    @Column(name = "requested_chunk_size")
    private Integer requestedChunkSize;

    @Column(name = "min_score")
    private Double minScore;

    @Column(name = "top_k")
    private Integer topK;

    @Column(name = "return_citations")
    private Boolean returnCitations;

    @Column(name = "alias_name", length = 100)
    private String aliasName;

    @Column(name = "old_index", length = 100)
    private String oldIndex;

    @Column(name = "new_index", length = 100)
    private String newIndex;

    @Column(name = "total_articles")
    private Integer totalArticles;

    @Column(name = "done_articles")
    private Integer doneArticles;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
