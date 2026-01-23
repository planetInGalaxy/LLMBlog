package com.lingdang.blog.model;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * 文章实体（扩展自原 BlogPost，增加 RAG 相关字段）
 */
@Data
@Entity
@Table(name = "articles", indexes = {
    @Index(name = "idx_slug", columnList = "slug", unique = true),
    @Index(name = "idx_status", columnList = "status"),
    @Index(name = "idx_published_at", columnList = "published_at")
})
public class Article {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * 文章标题
     */
    @Column(nullable = false, length = 200)
    private String title;
    
    /**
     * URL 友好的唯一标识符
     */
    @Column(nullable = false, unique = true, length = 200)
    private String slug;
    
    /**
     * 文章摘要
     */
    @Column(length = 500)
    private String summary;
    
    /**
     * Markdown 原文（事实源）
     */
    @Column(nullable = false, columnDefinition = "LONGTEXT")
    private String contentMarkdown;
    
    /**
     * 渲染后的 HTML（由后端生成，包含锚点）
     */
    @Column(columnDefinition = "LONGTEXT")
    private String contentHtml;
    
    /**
     * 内容哈希值（用于判断内容是否变化）
     */
    @Column(length = 64)
    private String contentHash;
    
    /**
     * 作者
     */
    @Column(length = 100)
    private String author;
    
    /**
     * 标签（逗号分隔）
     */
    @Column(length = 200)
    private String tags;
    
    /**
     * 封面图片 URL（外链或为空）
     */
    @Column(length = 500)
    private String coverUrl;
    
    /**
     * 文章状态
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ArticleStatus status = ArticleStatus.DRAFT;
    
    /**
     * 索引版本号（每次重新索引递增）
     */
    @Column(nullable = false)
    private Integer indexVersion = 0;
    
    /**
     * 浏览次数
     */
    @Column(name = "view_count", nullable = false)
    private Long viewCount = 0L;
    
    /**
     * 发布时间（status 变为 PUBLISHED 时写入）
     */
    @Column(name = "published_at")
    private LocalDateTime publishedAt;
    
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
