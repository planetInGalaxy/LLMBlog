package com.lingdang.blog.dto.article;

import com.lingdang.blog.model.ArticleStatus;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ArticleChunkDTO {
    private String chunkId;
    private Long articleId;
    private String slug;
    private String title;
    private String tags;
    private ArticleStatus status;
    private Integer indexVersion;
    private Integer headingLevel;
    private String headingText;
    private String anchor;
    private String chunkText;
    private Integer tokenCount;
    private Integer sequenceNumber;
    private LocalDateTime createdAt;
}
