package com.lingdang.blog.model;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * RAG 查询评估日志（用于调参与排障）
 */
@Data
@Entity
@Table(name = "rag_query_logs", indexes = {
    @Index(name = "idx_rag_query_request_id", columnList = "request_id", unique = true),
    @Index(name = "idx_rag_query_created_at", columnList = "created_at"),
    @Index(name = "idx_rag_query_success", columnList = "success")
})
public class RagQueryLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "request_id", nullable = false, unique = true, length = 100)
    private String requestId;

    @Column(name = "client_ip", length = 50)
    private String clientIp;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String question;

    @Column(name = "has_articles")
    private Boolean hasArticles;

    /**
     * 命中的文章 ID 列表（逗号分隔，便于快速判断召回是否命中预期文章）
     */
    @Column(name = "hit_article_ids", length = 500)
    private String hitArticleIds;

    // config snapshot
    @Column(name = "top_k")
    private Integer topK;

    @Column(name = "min_score")
    private Double minScore;

    @Column(name = "chunk_size")
    private Integer chunkSize;

    @Column(name = "return_citations")
    private Boolean returnCitations;

    @Column(name = "vector_weight")
    private Integer vectorWeight;

    @Column(name = "bm25_weight")
    private Integer bm25Weight;

    @Column(name = "bm25_max")
    private Double bm25Max;

    // metrics
    @Column(name = "vector_candidates")
    private Integer vectorCandidates;

    @Column(name = "bm25_candidates")
    private Integer bm25Candidates;

    @Column(name = "filtered_candidates")
    private Integer filteredCandidates;

    @Column(name = "citations_count")
    private Integer citationsCount;

    @Column(name = "retrieval_ms")
    private Integer retrievalMs;

    @Column(name = "latency_ms")
    private Integer latencyMs;

    @Column(nullable = false)
    private Boolean success = true;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
