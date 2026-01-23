package com.lingdang.blog.service;

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
    
    /**
     * 触发索引（发布时调用）
     */
    @Transactional
    public Long triggerIndex(Long articleId) {
        Article article = articleRepository.findById(articleId)
            .orElseThrow(() -> new RuntimeException("文章不存在: " + articleId));
        
        // 计算 content_hash
        String contentHash = DigestUtils.sha256Hex(article.getContentMarkdown());
        
        // 检查是否需要重新索引
        if (contentHash.equals(article.getContentHash())) {
            log.info("文章内容未变化，跳过索引: article_id={}", articleId);
            return null;
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
        Article article = articleRepository.findById(articleId)
            .orElseThrow(() -> new RuntimeException("文章不存在: " + articleId));
        
        // 强制更新 index_version
        article.setIndexVersion(article.getIndexVersion() + 1);
        
        // 重新计算 content_hash
        String contentHash = DigestUtils.sha256Hex(article.getContentMarkdown());
        article.setContentHash(contentHash);
        
        articleRepository.save(article);
        
        return triggerIndex(articleId);
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
