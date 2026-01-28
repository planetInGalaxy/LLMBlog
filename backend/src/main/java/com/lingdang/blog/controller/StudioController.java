package com.lingdang.blog.controller;

import com.lingdang.blog.config.ElasticsearchInitializer;
import com.lingdang.blog.dto.ApiResponse;
import com.lingdang.blog.dto.assistant.RagConfigDTO;
import com.lingdang.blog.dto.article.ArticleDTO;
import com.lingdang.blog.dto.article.StudioArticleUpsertRequest;
import com.lingdang.blog.service.ArticleService;
import com.lingdang.blog.service.ArticleSummaryJobService;
import com.lingdang.blog.service.IndexPipelineService;
import com.lingdang.blog.service.ArticleChunkService;
import com.lingdang.blog.dto.article.ArticleChunkDTO;
import com.lingdang.blog.model.ArticleSummaryJob;
import com.lingdang.blog.model.RagQueryHit;
import com.lingdang.blog.model.RagQueryLog;
import com.lingdang.blog.model.RagReindexJob;
import com.lingdang.blog.service.RagConfigService;
import com.lingdang.blog.service.RagReindexJobService;
import com.lingdang.blog.repository.RagQueryHitRepository;
import com.lingdang.blog.repository.RagQueryLogRepository;
import com.lingdang.blog.dto.studio.PromptTemplateDTO;
import com.lingdang.blog.service.PromptTemplateService;
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

    @Autowired
    private RagReindexJobService ragReindexJobService;

    @Autowired
    private PromptTemplateService promptTemplateService;

    @Autowired
    private RagQueryLogRepository ragQueryLogRepository;

    @Autowired
    private RagQueryHitRepository ragQueryHitRepository;

    @Autowired
    private ArticleChunkService articleChunkService;

    @Autowired
    private ArticleSummaryJobService articleSummaryJobService;
    
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
    public ResponseEntity<ApiResponse<ArticleDTO>> createArticle(@Valid @RequestBody StudioArticleUpsertRequest dto) {
        try {
            ArticleDTO created = articleService.createArticle(dto);

            // summary 为空时自动补齐
            boolean summaryEmpty = created.getSummary() == null || created.getSummary().trim().isEmpty();
            if (summaryEmpty) {
                articleSummaryJobService.submit(created.getId(), ArticleSummaryJob.Mode.FILL_IF_EMPTY);
            }

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
            @Valid @RequestBody StudioArticleUpsertRequest dto) {
        try {
            ArticleDTO updated = articleService.updateArticle(id, dto);

            boolean summaryEmpty = updated.getSummary() == null || updated.getSummary().trim().isEmpty();
            boolean regenerate = Boolean.TRUE.equals(dto.getRegenerateSummary());

            // 1) summary 为空：自动补齐
            if (summaryEmpty) {
                articleSummaryJobService.submit(id, ArticleSummaryJob.Mode.FILL_IF_EMPTY);
            }

            // 2) 显式开关：重生成摘要（允许覆盖）
            if (regenerate) {
                articleSummaryJobService.submit(id, ArticleSummaryJob.Mode.REGENERATE);
            }
            
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
        try {
            // 先检查 ES 连接
            ElasticsearchInitializer.IndexHealth health = esInitializer.checkIndexHealth();
            if (!health.isEsConnected()) {
                return ResponseEntity.ok(ApiResponse.error("Elasticsearch 连接失败，请检查服务状态"));
            }

            // 如果 alias 不存在，先初始化（会创建索引并绑定 alias）
            if (!health.isIndexExists()) {
                esInitializer.initializeIndex();
            }

            // 走蓝绿重建（FullReindexService），避免重建期间线上索引抖动
            RagConfigDTO cfg = ragConfigService.getConfig();
            ragReindexJobService.submitChunkSizeReindex(cfg);

            return ResponseEntity.ok(ApiResponse.success("已提交全量重建索引任务（蓝绿重建）", null));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error(e.getMessage()));
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
     * 最近一次重建索引任务
     */
    @GetMapping("/reindex-jobs/latest")
    public ResponseEntity<ApiResponse<RagReindexJob>> latestReindexJob() {
        return ResponseEntity.ok(ApiResponse.success(ragReindexJobService.getLatestJob()));
    }

    /**
     * 最近 7 天重建索引任务列表
     */
    @GetMapping("/reindex-jobs")
    public ResponseEntity<ApiResponse<List<RagReindexJob>>> listReindexJobs() {
        return ResponseEntity.ok(ApiResponse.success(ragReindexJobService.listRecentJobs()));
    }

    /**
     * 最近 7 天 RAG 查询日志
     */
    @GetMapping("/rag-logs")
    public ResponseEntity<ApiResponse<List<RagQueryLog>>> listRagLogs() {
        return ResponseEntity.ok(ApiResponse.success(
            ragQueryLogRepository.findByCreatedAtAfterOrderByCreatedAtDesc(
                java.time.LocalDateTime.now().minusDays(7)
            )
        ));
    }

    /**
     * RAG 查询详情（含命中 chunks）
     */
    @GetMapping("/rag-logs/{requestId}")
    public ResponseEntity<ApiResponse<java.util.Map<String, Object>>> ragLogDetail(@PathVariable String requestId) {
        RagQueryLog log = ragQueryLogRepository.findByRequestId(requestId).orElse(null);
        List<RagQueryHit> hits = ragQueryHitRepository.findByRequestIdOrderByRankNoAsc(requestId);
        java.util.Map<String, Object> data = new java.util.HashMap<>();
        data.put("log", log);
        data.put("hits", hits);
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    /**
     * 查看当前 chunks 切片（来自 MySQL article_chunks 表）
     */
    @GetMapping("/chunks")
    public ResponseEntity<ApiResponse<List<ArticleChunkDTO>>> listChunks(
            @RequestParam(required = false) Long articleId,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer pageSize) {
        return ResponseEntity.ok(ApiResponse.success(articleChunkService.listChunks(articleId, page, pageSize)));
    }

    /**
     * 查看指定文章的 chunks 切片（按序号升序）
     */
    @GetMapping("/articles/{id}/chunks")
    public ResponseEntity<ApiResponse<List<ArticleChunkDTO>>> listChunksByArticle(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(articleChunkService.listChunksByArticleId(id)));
    }

    /**
     * 获取 RAG 配置
     */
    @GetMapping("/rag-config")
    public ResponseEntity<ApiResponse<RagConfigDTO>> getRagConfig() {
        return ResponseEntity.ok(ApiResponse.success(ragConfigService.getConfig()));
    }

    /**
     * 获取所有提示词配置
     */
    @GetMapping("/prompts")
    public ResponseEntity<ApiResponse<java.util.List<PromptTemplateDTO>>> listPrompts() {
        return ResponseEntity.ok(ApiResponse.success(promptTemplateService.listAll()));
    }

    /**
     * 更新某个提示词
     */
    @PutMapping("/prompts")
    public ResponseEntity<ApiResponse<PromptTemplateDTO>> updatePrompt(@RequestBody PromptTemplateDTO dto) {
        try {
            return ResponseEntity.ok(ApiResponse.success("保存成功", promptTemplateService.update(dto)));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * 更新 RAG 配置
     */
    @PutMapping("/rag-config")
    public ResponseEntity<ApiResponse<RagConfigDTO>> updateRagConfig(@RequestBody RagConfigDTO request) {
        try {
            log.info("收到 rag-config 更新请求: topK={}, minScore={}, chunkSize={}, vectorWeight={}, bm25Weight={}, bm25Max={}, returnCitations={}",
                request != null ? request.getTopK() : null,
                request != null ? request.getMinScore() : null,
                request != null ? request.getChunkSize() : null,
                request != null ? request.getVectorWeight() : null,
                request != null ? request.getBm25Weight() : null,
                request != null ? request.getBm25Max() : null,
                request != null ? request.getReturnCitations() : null);

            // 先读取当前配置，判断 chunkSize 是否变化
            RagConfigDTO current = ragConfigService.getConfig();
            boolean chunkSizeChanged = request != null
                && request.getChunkSize() != null
                && (current == null || !request.getChunkSize().equals(current.getChunkSize()));

            // 更新立即生效参数（不包含 chunkSize）
            RagConfigDTO safeRequest = request;
            if (safeRequest != null) {
                RagConfigDTO tmp = new RagConfigDTO();
                tmp.setTopK(safeRequest.getTopK());
                tmp.setMinScore(safeRequest.getMinScore());
                tmp.setReturnCitations(safeRequest.getReturnCitations());
                tmp.setVectorWeight(safeRequest.getVectorWeight());
                tmp.setBm25Weight(safeRequest.getBm25Weight());
                tmp.setBm25Max(safeRequest.getBm25Max());
                // 注意：chunkSize 不在这里落库，等待异步重建成功后由任务落库
                safeRequest = tmp;
            }

            RagConfigDTO updated = ragConfigService.updateConfigAndReindexIfNeeded(safeRequest);

            if (chunkSizeChanged) {
                RagConfigDTO requested = new RagConfigDTO();
                requested.setTopK(updated.getTopK());
                requested.setMinScore(updated.getMinScore());
                requested.setReturnCitations(updated.getReturnCitations());
                requested.setVectorWeight(updated.getVectorWeight());
                requested.setBm25Weight(updated.getBm25Weight());
                requested.setBm25Max(updated.getBm25Max());
                requested.setChunkSize(request.getChunkSize());

                RagReindexJob job = ragReindexJobService.submitChunkSizeReindex(requested);
                log.info("chunkSize 变更触发异步重建任务: job_id={}, {} -> {}",
                    job.getId(), current != null ? current.getChunkSize() : null, request.getChunkSize());

                // 返回给前端：配置已保存（除 chunkSize 外），chunkSize 会在重建成功后生效
                return ResponseEntity.ok(ApiResponse.success("已提交重建索引任务，chunkSize 将在任务成功后生效", updated));
            }

            log.info("rag-config 更新完成: topK={}, minScore={}, chunkSize={}, vectorWeight={}, bm25Weight={}, bm25Max={}, returnCitations={}",
                updated.getTopK(), updated.getMinScore(), updated.getChunkSize(),
                updated.getVectorWeight(), updated.getBm25Weight(), updated.getBm25Max(), updated.getReturnCitations());
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
