package com.lingdang.blog.controller;

import com.lingdang.blog.dto.ApiResponse;
import com.lingdang.blog.dto.article.ArticleDTO;
import com.lingdang.blog.service.ArticleService;
import com.lingdang.blog.service.IndexPipelineService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Studio 管理 Controller
 */
@RestController
@RequestMapping("/api/studio")
@CrossOrigin(origins = "*")
public class StudioController {
    
    @Autowired
    private ArticleService articleService;
    
    @Autowired
    private IndexPipelineService indexPipelineService;
    
    /**
     * 获取所有文章（含草稿）
     */
    @GetMapping("/articles")
    public ResponseEntity<ApiResponse<List<ArticleDTO>>> getAllArticles() {
        List<ArticleDTO> articles = articleService.getAllArticles();
        return ResponseEntity.ok(ApiResponse.success(articles));
    }
    
    /**
     * 获取文章详情
     */
    @GetMapping("/articles/{id}")
    public ResponseEntity<ApiResponse<ArticleDTO>> getArticle(@PathVariable Long id) {
        return articleService.getArticleById(id)
            .map(article -> ResponseEntity.ok(ApiResponse.success(article)))
            .orElse(ResponseEntity.ok(ApiResponse.error("文章不存在")));
    }
    
    /**
     * 创建文章
     */
    @PostMapping("/articles")
    public ResponseEntity<ApiResponse<ArticleDTO>> createArticle(@Valid @RequestBody ArticleDTO dto) {
        try {
            ArticleDTO created = articleService.createArticle(dto);
            return ResponseEntity.ok(ApiResponse.success("文章创建成功", created));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error(e.getMessage()));
        }
    }
    
    /**
     * 更新文章
     */
    @PutMapping("/articles/{id}")
    public ResponseEntity<ApiResponse<ArticleDTO>> updateArticle(
            @PathVariable Long id,
            @Valid @RequestBody ArticleDTO dto) {
        try {
            ArticleDTO updated = articleService.updateArticle(id, dto);
            return ResponseEntity.ok(ApiResponse.success("文章更新成功", updated));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error(e.getMessage()));
        }
    }
    
    /**
     * 发布文章
     */
    @PutMapping("/articles/{id}/publish")
    public ResponseEntity<ApiResponse<ArticleDTO>> publishArticle(@PathVariable Long id) {
        try {
            ArticleDTO published = articleService.publishArticle(id);
            // 触发索引流水线
            Long jobId = indexPipelineService.triggerIndex(id);
            return ResponseEntity.ok(ApiResponse.success("文章发布成功，索引任务已提交", published));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error(e.getMessage()));
        }
    }
    
    /**
     * 下线文章
     */
    @PutMapping("/articles/{id}/offline")
    public ResponseEntity<ApiResponse<ArticleDTO>> offlineArticle(@PathVariable Long id) {
        try {
            ArticleDTO offline = articleService.offlineArticle(id);
            // TODO: 从 ES 删除
            return ResponseEntity.ok(ApiResponse.success("文章下线成功", offline));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error(e.getMessage()));
        }
    }
    
    /**
     * 删除文章
     */
    @DeleteMapping("/articles/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteArticle(@PathVariable Long id) {
        try {
            articleService.deleteArticle(id);
            return ResponseEntity.ok(ApiResponse.success("文章删除成功", null));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error(e.getMessage()));
        }
    }
    
    /**
     * 重新索引
     */
    @PostMapping("/articles/{id}/reindex")
    public ResponseEntity<ApiResponse<Void>> reindexArticle(@PathVariable Long id) {
        try {
            Long jobId = indexPipelineService.reindex(id);
            return ResponseEntity.ok(ApiResponse.success("索引任务已提交", null));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error(e.getMessage()));
        }
    }
    
    /**
     * 全量重建索引
     */
    @PostMapping("/reindex-all")
    public ResponseEntity<ApiResponse<Void>> reindexAll() {
        try {
            List<ArticleDTO> articles = articleService.getPublishedArticles();
            for (ArticleDTO article : articles) {
                indexPipelineService.reindex(article.getId());
            }
            return ResponseEntity.ok(ApiResponse.success("已提交 " + articles.size() + " 个索引任务", null));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error(e.getMessage()));
        }
    }
}
