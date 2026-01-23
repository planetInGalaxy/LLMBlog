package com.lingdang.blog.model;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * 文章片段实体（存储切分后的 chunk，作为 ES 索引的事实源）
 */
@Data
@Entity
@Table(name = "article_chunks", indexes = {
    @Index(name = "idx_chunk_id", columnList = "chunk_id", unique = true),
    @Index(name = "idx_article_id", columnList = "article_id"),
    @Index(name = "idx_index_version", columnList = "article_id,index_version")
})
public class ArticleChunk {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * Chunk 唯一标识符（格式：article_id_001）
     */
    @Column(name = "chunk_id", nullable = false, unique = true, length = 100)
    private String chunkId;
    
    /**
     * 关联的文章 ID
     */
    @Column(name = "article_id", nullable = false)
    private Long articleId;
    
    /**
     * 文章 slug（冗余存储，便于检索）
     */
    @Column(nullable = false, length = 200)
    private String slug;
    
    /**
     * 文章标题（冗余存储）
     */
    @Column(nullable = false, length = 200)
    private String title;
    
    /**
     * 标签（冗余存储）
     */
    @Column(length = 200)
    private String tags;
    
    /**
     * 文章状态（冗余存储）
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ArticleStatus status;
    
    /**
     * 索引版本号（与 article.index_version 一致）
     */
    @Column(name = "index_version", nullable = false)
    private Integer indexVersion;
    
    /**
     * 标题层级（1-6，对应 H1-H6）
     */
    @Column(name = "heading_level")
    private Integer headingLevel;
    
    /**
     * 标题文本
     */
    @Column(name = "heading_text", length = 500)
    private String headingText;
    
    /**
     * 锚点（用于跳转，与前端一致）
     */
    @Column(length = 200)
    private String anchor;
    
    /**
     * Chunk 文本内容
     */
    @Column(name = "chunk_text", nullable = false, columnDefinition = "TEXT")
    private String chunkText;
    
    /**
     * Token 数量（估算）
     */
    @Column(name = "token_count")
    private Integer tokenCount;
    
    /**
     * Chunk 在文章中的序号（从 1 开始）
     */
    @Column(name = "sequence_number", nullable = false)
    private Integer sequenceNumber;
    
    /**
     * 创建时间
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
