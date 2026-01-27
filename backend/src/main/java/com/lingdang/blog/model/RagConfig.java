package com.lingdang.blog.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * RAG 配置（持久化）
 *
 * 约定：仅使用单行配置（id=1）。
 */
@Data
@Entity
@Table(name = "rag_config")
public class RagConfig {

    @Id
    private Long id;

    @Column(name = "top_k")
    private Integer topK;

    @Column(name = "min_score")
    private Double minScore;

    /**
     * chunkSize 目前仅展示用；实际切分粒度需要重建索引。
     */
    @Column(name = "chunk_size")
    private Integer chunkSize;

    @Column(name = "return_citations")
    private Boolean returnCitations;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
