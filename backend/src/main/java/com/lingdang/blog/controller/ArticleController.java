package com.lingdang.blog.controller;

import com.lingdang.blog.dto.ApiResponse;
import com.lingdang.blog.dto.article.ArticleDTO;
import com.lingdang.blog.service.ArticleService;
import com.lingdang.blog.service.ArticleSearchService;
import com.lingdang.blog.dto.article.ArticleSearchResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 文章公开 API Controller
 */
@RestController
@RequestMapping("/api/articles")
@CrossOrigin(origins = "*")
public class ArticleController {
    
    @Autowired
    private ArticleService articleService;

    @Autowired
    private ArticleSearchService articleSearchService;
    
    /**
     * 获取已发布文章列表
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<ArticleDTO>>> getPublishedArticles(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer pageSize,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortOrder) {
        List<ArticleDTO> articles = articleService.getPublishedArticles(page, pageSize, sortBy, sortOrder);
        return ResponseEntity.ok(ApiResponse.success(articles));
    }
    
    /**
     * 根据 Slug 获取文章详情
     */
    @GetMapping("/{slug}")
    public ResponseEntity<ApiResponse<ArticleDTO>> getArticleBySlug(@PathVariable String slug) {
        return articleService.getArticleBySlug(slug)
            .map(article -> {
                // 增加浏览次数
                articleService.incrementViewCount(article.getId());
                return ResponseEntity.ok(ApiResponse.success(article));
            })
            .orElse(ResponseEntity.ok(ApiResponse.error("文章不存在")));
    }
    
    /**
     * 搜索文章（基于 ES chunks，返回文章维度结果，包含高亮片段 snippet）
     */
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<ArticleSearchResponse>> searchArticles(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer pageSize) {
        String query = (q != null && !q.trim().isEmpty()) ? q : keyword;
        ArticleSearchResponse resp = articleSearchService.searchPublished(query, page, pageSize);
        return ResponseEntity.ok(ApiResponse.success(resp));
    }
}
