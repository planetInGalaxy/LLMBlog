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

    /**
     * 是否开启灵活模式（FLEXIBLE）。
     * - true：无命中文章时也允许基于模型知识回答
     * - false：强制只基于文章（等价于 ARTICLE_ONLY）
     */
    @Column(name = "flexible_mode_enabled")
    private Boolean flexibleModeEnabled;

    /**
     * 混合检索权重：向量相似度权重（0~100）
     */
    @Column(name = "vector_weight")
    private Integer vectorWeight;

    /**
     * 混合检索权重：BM25 权重（0~100）
     */
    @Column(name = "bm25_weight")
    private Integer bm25Weight;

    /**
     * BM25 归一化上限（建议取 bm25 的 P95/P99）。
     */
    @Column(name = "bm25_max")
    private Double bm25Max;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
