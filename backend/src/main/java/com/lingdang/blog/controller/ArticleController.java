package com.lingdang.blog.controller;

import com.lingdang.blog.dto.ApiResponse;
import com.lingdang.blog.dto.article.ArticleDTO;
import com.lingdang.blog.service.ArticleService;
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
     * 搜索文章
     */
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<ArticleDTO>>> searchArticles(@RequestParam String keyword) {
        List<ArticleDTO> articles = articleService.searchArticles(keyword);
        return ResponseEntity.ok(ApiResponse.success(articles));
    }
}
