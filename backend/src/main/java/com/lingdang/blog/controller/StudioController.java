package com.lingdang.blog.controller;

import com.lingdang.blog.config.ElasticsearchInitializer;
import com.lingdang.blog.dto.ApiResponse;
import com.lingdang.blog.dto.assistant.RagConfigDTO;
import com.lingdang.blog.dto.article.ArticleDTO;
import com.lingdang.blog.service.ArticleService;
import com.lingdang.blog.service.IndexPipelineService;
import com.lingdang.blog.service.RagConfigService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Studio 管理 Controller
 */
@Slf4j
@RestController
@RequestMapping("/api/studio")
@CrossOrigin(origins = "*")
public class StudioController {
    
    @Autowired
    private ArticleService articleService;
    
    @Autowired
    private IndexPipelineService indexPipelineService;
    
    @Autowired
    private ElasticsearchInitializer esInitializer;

    @Autowired
    private RagConfigService ragConfigService;
    
    /**
     * 获取所有文章（含草稿）
     */
    @GetMapping("/articles")
    public ResponseEntity<ApiResponse<List<ArticleDTO>>> getAllArticles(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer pageSize,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortOrder) {
        List<ArticleDTO> articles = articleService.getAllArticles(page, pageSize, sortBy, sortOrder);
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
            
            // 如果是已发布状态，强制触发重新索引（因为可能修改了标题、标签等元数据）
            if ("PUBLISHED".equals(updated.getStatus())) {
                indexPipelineService.triggerIndex(id, true);  // force=true，强制更新
            }
            
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
            // 从 ES 删除索引
            indexPipelineService.deleteIndex(id);
            return ResponseEntity.ok(ApiResponse.success("文章下线成功，已从检索索引中移除", offline));
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
            // 先从 ES 删除索引
            indexPipelineService.deleteIndex(id);
            // 再删除文章本身
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
        if (!indexPipelineService.tryStartFullReindex()) {
            return ResponseEntity.ok(ApiResponse.error("全量重建索引已在进行"));
        }

        try {
            // 先检查索引健康
            ElasticsearchInitializer.IndexHealth health = esInitializer.checkIndexHealth();
            if (!health.isEsConnected()) {
                return ResponseEntity.ok(ApiResponse.error("Elasticsearch 连接失败，请检查服务状态"));
            }
            
            // 如果索引不存在，先初始化
            if (!health.isIndexExists()) {
                esInitializer.initializeIndex();
            }
            
            List<ArticleDTO> articles = articleService.getPublishedArticles();
            int submitted = 0;
            for (ArticleDTO article : articles) {
                Long jobId = indexPipelineService.reindex(article.getId());
                if (jobId != null) {
                    indexPipelineService.trackFullReindexJob(jobId);
                    submitted++;
                }
            }
            if (submitted == 0) {
                indexPipelineService.finishFullReindexIfNoJobs();
            }
            return ResponseEntity.ok(ApiResponse.success("已提交 " + submitted + " 个索引任务", null));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error(e.getMessage()));
        } finally {
            indexPipelineService.finishFullReindexIfNoJobs();
        }
    }
    
    /**
     * 检查索引健康状态
     */
    @GetMapping("/index-health")
    public ResponseEntity<ApiResponse<ElasticsearchInitializer.IndexHealth>> checkIndexHealth() {
        try {
            ElasticsearchInitializer.IndexHealth health = esInitializer.checkIndexHealth();
            return ResponseEntity.ok(ApiResponse.success(health));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error("检查失败: " + e.getMessage()));
        }
    }

    /**
     * 获取 RAG 配置
     */
    @GetMapping("/rag-config")
    public ResponseEntity<ApiResponse<RagConfigDTO>> getRagConfig() {
        return ResponseEntity.ok(ApiResponse.success(ragConfigService.getConfig()));
    }

    /**
     * 更新 RAG 配置
     */
    @PutMapping("/rag-config")
    public ResponseEntity<ApiResponse<RagConfigDTO>> updateRagConfig(@RequestBody RagConfigDTO request) {
        try {
            log.info("收到 rag-config 更新请求: topK={}, minScore={}, chunkSize={}, returnCitations={}",
                request != null ? request.getTopK() : null,
                request != null ? request.getMinScore() : null,
                request != null ? request.getChunkSize() : null,
                request != null ? request.getReturnCitations() : null);

            RagConfigDTO updated = ragConfigService.updateConfigAndReindexIfNeeded(request);
            log.info("rag-config 更新完成: topK={}, minScore={}, chunkSize={}, returnCitations={}",
                updated.getTopK(), updated.getMinScore(), updated.getChunkSize(), updated.getReturnCitations());
            return ResponseEntity.ok(ApiResponse.success("保存成功", updated));
        } catch (IllegalArgumentException e) {
            log.warn("rag-config 更新参数错误: {}", e.getMessage());
            return ResponseEntity.ok(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("rag-config 更新失败", e);
            return ResponseEntity.ok(ApiResponse.error("保存失败: " + e.getMessage()));
        }
    }
}
