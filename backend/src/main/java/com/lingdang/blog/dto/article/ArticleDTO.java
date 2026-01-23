package com.lingdang.blog.dto.article;

import com.lingdang.blog.model.ArticleStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 文章 DTO
 */
@Data
public class ArticleDTO {
    
    private Long id;
    
    @NotBlank(message = "标题不能为空")
    @Size(max = 200, message = "标题长度不能超过200字符")
    private String title;
    
    @NotBlank(message = "Slug 不能为空")
    @Size(max = 200, message = "Slug 长度不能超过200字符")
    private String slug;
    
    @Size(max = 500, message = "摘要长度不能超过500字符")
    private String summary;
    
    @NotBlank(message = "内容不能为空")
    private String contentMarkdown;
    
    private String contentHtml;
    
    private String contentHash;
    
    private String author;
    
    private String tags;
    
    private String coverUrl;
    
    private ArticleStatus status;
    
    private Integer indexVersion;
    
    private Long viewCount;
    
    private LocalDateTime publishedAt;
    
    private LocalDateTime createdAt;
    
    private LocalDateTime updatedAt;
}
