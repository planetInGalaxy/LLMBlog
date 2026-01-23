package com.lingdang.blog.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import com.lingdang.blog.model.*;
import com.lingdang.blog.repository.ArticleChunkRepository;
import com.lingdang.blog.repository.ArticleRepository;
import com.lingdang.blog.repository.RagIndexJobRepository;
import com.lingdang.blog.repository.elasticsearch.ChunkDocumentRepository;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 索引流水线服务
 */
@Slf4j
@Service
public class IndexPipelineService {
    
    @Autowired
    private ArticleRepository articleRepository;
    
    @Autowired
    private ArticleChunkRepository articleChunkRepository;
    
    @Autowired
    private RagIndexJobRepository ragIndexJobRepository;
    
    @Autowired
    private ChunkDocumentRepository chunkDocumentRepository;
    
    @Autowired
    private MarkdownService markdownService;
    
    @Autowired
    private ChunkService chunkService;
    
    @Autowired
    private LlmService llmService;
    
    @Autowired
    private ElasticsearchClient esClient;
    
    private static final String INDEX_NAME = "lingdang_chunks_v1";
    
    /**
     * 触发索引（发布时调用）- 会检查内容是否变化
     */
    @Transactional
    public Long triggerIndex(Long articleId) {
        return triggerIndex(articleId, false);
    }
    
    /**
     * 触发索引
     * @param articleId 文章 ID
     * @param force 是否强制索引（true=不检查内容变化，false=检查内容变化）
     */
    @Transactional
    public Long triggerIndex(Long articleId, boolean force) {
        Article article = articleRepository.findById(articleId)
            .orElseThrow(() -> new RuntimeException("文章不存在: " + articleId));
        
        // 检查 ES 索引是否存在
        try {
            boolean indexExists = esClient.indices().exists(e -> e.index(INDEX_NAME)).value();
            if (!indexExists) {
                log.warn("ES 索引不存在，索引任务可能失败: {}", INDEX_NAME);
                log.warn("请在 Studio 执行「全量重建索引」来创建索引");
            }
        } catch (Exception e) {
            log.error("检查 ES 索引失败: {}", e.getMessage());
        }
        
        // 计算 content_hash
        String contentHash = DigestUtils.sha256Hex(article.getContentMarkdown());
        
        // 非强制模式：检查内容是否变化
        if (!force && contentHash.equals(article.getContentHash())) {
            log.info("文章内容未变化，跳过索引: article_id={}", articleId);
            return null;
        }
        
        // 强制模式：记录日志
        if (force) {
            log.info("强制重新索引: article_id={}", articleId);
        }
        
        // 检查是否有正在运行的任务
        ragIndexJobRepository.findFirstByArticleIdAndStatus(articleId, IndexJobStatus.RUNNING)
            .ifPresent(job -> {
                throw new RuntimeException("文章正在索引中，请稍后再试");
            });
        
        // 更新 content_hash 和 index_version
        article.setContentHash(contentHash);
        article.setIndexVersion(article.getIndexVersion() + 1);
        
        // 生成 content_html
        String contentHtml = markdownService.markdownToHtml(article.getContentMarkdown());
        article.setContentHtml(contentHtml);
        
        articleRepository.save(article);
        
        // 创建索引任务
        RagIndexJob job = new RagIndexJob();
        job.setArticleId(articleId);
        job.setStatus(IndexJobStatus.PENDING);
        job.setTargetIndexVersion(article.getIndexVersion());
        job.setRetryCount(0);
        
        RagIndexJob savedJob = ragIndexJobRepository.save(job);
        log.info("创建索引任务: job_id={}, article_id={}, version={}", 
            savedJob.getId(), articleId, article.getIndexVersion());
        
        // 异步执行索引
        executeIndexAsync(savedJob.getId());
        
        return savedJob.getId();
    }
    
    /**
     * 异步执行索引
     */
    @Async
    public void executeIndexAsync(Long jobId) {
        try {
            executeIndex(jobId);
        } catch (Exception e) {
            log.error("索引任务执行失败: job_id={}", jobId, e);
        }
    }
    
    /**
     * 执行索引（核心流水线）
     */
    @Transactional
    public void executeIndex(Long jobId) {
        RagIndexJob job = ragIndexJobRepository.findById(jobId)
            .orElseThrow(() -> new RuntimeException("索引任务不存在: " + jobId));
        
        try {
            // 标记为运行中
            job.setStatus(IndexJobStatus.RUNNING);
            job.setStartedAt(LocalDateTime.now());
            ragIndexJobRepository.save(job);
            
            // 1. 获取文章
            Article article = articleRepository.findById(job.getArticleId())
                .orElseThrow(() -> new RuntimeException("文章不存在"));
            
            log.info("开始索引: article_id={}, version={}", article.getId(), article.getIndexVersion());
            
            // 2. 切分 chunk
            List<ArticleChunk> chunks = chunkService.splitArticle(article);
            job.setChunksGenerated(chunks.size());
            ragIndexJobRepository.save(job);
            
            // 3. 删除旧 chunks（MySQL）
            chunkService.deleteChunksByArticleId(article.getId());
            
            // 4. 保存新 chunks（MySQL）
            chunkService.saveChunks(chunks);
            
            // 5. 生成 embeddings
            List<ChunkDocument> documents = new ArrayList<>();
            for (ArticleChunk chunk : chunks) {
                try {
                    float[] embedding = llmService.generateEmbedding(chunk.getChunkText());
                    
                    ChunkDocument doc = new ChunkDocument();
                    doc.setChunkId(chunk.getChunkId());
                    doc.setArticleId(chunk.getArticleId());
                    doc.setSlug(chunk.getSlug());
                    doc.setTitle(chunk.getTitle());
                    doc.setTags(chunk.getTags());
                    doc.setStatus(chunk.getStatus().name());
                    doc.setIndexVersion(chunk.getIndexVersion());
                    doc.setHeadingLevel(chunk.getHeadingLevel());
                    doc.setHeadingText(chunk.getHeadingText());
                    doc.setAnchor(chunk.getAnchor());
                    doc.setChunkText(chunk.getChunkText());
                    doc.setEmbedding(embedding);
                    doc.setTokenCount(chunk.getTokenCount());
                    doc.setSequenceNumber(chunk.getSequenceNumber());
                    
                    documents.add(doc);
                } catch (Exception e) {
                    log.error("生成 embedding 失败: chunk_id={}", chunk.getChunkId(), e);
                    throw e;
                }
            }
            
            // 6. 删除 ES 中的旧文档
            try {
                chunkDocumentRepository.deleteByArticleId(article.getId());
            } catch (Exception e) {
                log.warn("删除 ES 旧文档失败（可能不存在）: article_id={}", article.getId());
            }
            
            // 7. 写入 ES
            chunkDocumentRepository.saveAll(documents);
            job.setChunksIndexed(documents.size());
            
            // 8. 标记为成功
            job.setStatus(IndexJobStatus.SUCCESS);
            job.setCompletedAt(LocalDateTime.now());
            ragIndexJobRepository.save(job);
            
            log.info("索引完成: article_id={}, chunks={}, version={}", 
                article.getId(), chunks.size(), article.getIndexVersion());
            
        } catch (Exception e) {
            // 标记为失败
            job.setStatus(IndexJobStatus.FAILED);
            job.setErrorMessage(e.getMessage());
            job.setErrorStack(getStackTrace(e));
            job.setCompletedAt(LocalDateTime.now());
            ragIndexJobRepository.save(job);
            
            log.error("索引失败: job_id={}, article_id={}", jobId, job.getArticleId(), e);
            throw new RuntimeException("索引失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 重新索引（强制）
     */
    @Transactional
    public Long reindex(Long articleId) {
        log.info("执行强制重新索引: article_id={}", articleId);
        
        Article article = articleRepository.findById(articleId)
            .orElseThrow(() -> new RuntimeException("文章不存在: " + articleId));
        
        // 检查文章状态，只索引已发布的文章
        if (article.getStatus() != ArticleStatus.PUBLISHED) {
            log.warn("文章未发布，跳过索引: article_id={}, status={}", articleId, article.getStatus());
            throw new RuntimeException("只能对已发布的文章执行索引操作");
        }
        
        // 强制索引：使用 force=true，跳过内容变化检查
        return triggerIndex(articleId, true);
    }
    
    /**
     * 删除索引（下线或删除文章时调用）
     */
    @Transactional
    public void deleteIndex(Long articleId) {
        try {
            // 1. 从 ES 删除
            chunkDocumentRepository.deleteByArticleId(articleId);
            log.info("从 ES 删除文章索引: article_id={}", articleId);
            
            // 2. 从 MySQL 删除 chunks
            chunkService.deleteChunksByArticleId(articleId);
            log.info("从 MySQL 删除文章 chunks: article_id={}", articleId);
            
        } catch (Exception e) {
            log.error("删除索引失败: article_id={}", articleId, e);
            // 不抛出异常，避免影响文章的下线/删除操作
        }
    }
    
    /**
     * 获取异常堆栈
     */
    private String getStackTrace(Exception e) {
        StringBuilder sb = new StringBuilder();
        sb.append(e.getMessage()).append("\n");
        for (StackTraceElement element : e.getStackTrace()) {
            sb.append("\tat ").append(element.toString()).append("\n");
            if (sb.length() > 1000) break; // 限制长度
        }
        return sb.toString();
    }
}
