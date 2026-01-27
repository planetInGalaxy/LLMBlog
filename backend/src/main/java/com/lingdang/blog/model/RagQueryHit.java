package com.lingdang.blog.model;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * RAG 命中详情（按请求记录 topN chunks）
 */
@Data
@Entity
@Table(name = "rag_query_hits", indexes = {
    @Index(name = "idx_rag_hit_request_id", columnList = "request_id"),
    @Index(name = "idx_rag_hit_created_at", columnList = "created_at")
})
public class RagQueryHit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "request_id", nullable = false, length = 100)
    private String requestId;

    @Column(name = "rank_no")
    private Integer rankNo;

    @Column(name = "chunk_id", length = 100)
    private String chunkId;

    @Column(name = "article_id")
    private Long articleId;

    @Column(name = "slug", length = 255)
    private String slug;

    @Column(name = "title", length = 500)
    private String title;

    @Column(name = "vector_score")
    private Double vectorScore;

    @Column(name = "bm25_score")
    private Double bm25Score;

    @Column(name = "final_score")
    private Double finalScore;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
