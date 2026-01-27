package com.lingdang.blog.service;

import com.lingdang.blog.config.ElasticsearchInitializer;
import com.lingdang.blog.dto.assistant.RagConfigDTO;
import com.lingdang.blog.model.RagConfig;
import com.lingdang.blog.model.RagReindexJob;
import com.lingdang.blog.repository.RagConfigRepository;
import com.lingdang.blog.repository.RagReindexJobRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 异步全量重建索引任务服务
 */
@Slf4j
@Service
public class RagReindexJobService {

    private static final int RETENTION_DAYS = 7;

    @Autowired
    private RagReindexJobRepository ragReindexJobRepository;

    @Autowired
    private FullReindexService fullReindexService;

    @Autowired
    private RagConfigRepository ragConfigRepository;

    @Autowired
    private RagConfigService ragConfigService;

    @Autowired
    private ElasticsearchInitializer esInitializer;

    @Transactional
    public RagReindexJob submitChunkSizeReindex(RagConfigDTO requestedConfig) {
        RagReindexJob job = new RagReindexJob();
        job.setStatus(RagReindexJob.Status.PENDING);
        job.setRequestedChunkSize(requestedConfig.getChunkSize());
        job.setMinScore(requestedConfig.getMinScore());
        job.setTopK(requestedConfig.getTopK());
        job.setReturnCitations(requestedConfig.getReturnCitations());
        job.setAliasName(ElasticsearchInitializer.INDEX_ALIAS);
        job.setOldIndex(esInitializer.resolveCurrentIndex());
        RagReindexJob saved = ragReindexJobRepository.save(job);

        runJobAsync(saved.getId());
        return saved;
    }

    @Async("indexTaskExecutor")
    public void runJobAsync(Long jobId) {
        executeJob(jobId);
    }

    @Transactional
    public void executeJob(Long jobId) {
        RagReindexJob job = ragReindexJobRepository.findById(jobId)
            .orElseThrow(() -> new RuntimeException("任务不存在: " + jobId));

        if (job.getStatus() == RagReindexJob.Status.RUNNING) return;

        job.setStatus(RagReindexJob.Status.RUNNING);
        job.setStartedAt(LocalDateTime.now());
        ragReindexJobRepository.save(job);

        try {
            int chunkSize = job.getRequestedChunkSize() != null ? job.getRequestedChunkSize() : 900;
            ChunkingOptions options = ChunkingOptions.of(
                Math.max(200, (int) Math.round(chunkSize * 0.67)),
                chunkSize,
                Math.min(200, Math.max(0, (int) Math.round(chunkSize * 0.10)))
            );

            // 执行蓝绿重建（在 FullReindexService 内部创建新索引并切换 alias）
            fullReindexService.rebuildAllPublishedToNewIndex(options,
                (total, done) -> {
                    try {
                        updateProgress(jobId, total, done);
                    } catch (Exception ignored) {
                    }
                },
                (newIndex) -> {
                    try {
                        updateNewIndex(jobId, newIndex);
                    } catch (Exception ignored) {
                    }
                }
            );

            // 重建成功后才落库配置（只更新 chunkSize；其他字段本身不需要重建，可按需扩展）
            applyChunkSizeToConfig(job.getRequestedChunkSize());

            RagReindexJob finished = ragReindexJobRepository.findById(jobId).orElseThrow();
            finished.setStatus(RagReindexJob.Status.SUCCESS);
            finished.setCompletedAt(LocalDateTime.now());
            ragReindexJobRepository.save(finished);

            log.info("全量重建索引任务成功: job_id={}, new_index={}", jobId, finished.getNewIndex());

        } catch (Exception e) {
            RagReindexJob failed = ragReindexJobRepository.findById(jobId).orElseThrow();
            failed.setStatus(RagReindexJob.Status.FAILED);
            failed.setCompletedAt(LocalDateTime.now());
            failed.setErrorMessage(e.getMessage());
            ragReindexJobRepository.save(failed);
            log.error("全量重建索引任务失败: job_id={}", jobId, e);
        }
    }

    @Transactional
    protected void updateProgress(Long jobId, int totalArticles, int doneArticles) {
        RagReindexJob job = ragReindexJobRepository.findById(jobId).orElseThrow();
        job.setTotalArticles(totalArticles);
        job.setDoneArticles(doneArticles);
        ragReindexJobRepository.save(job);
    }

    @Transactional
    protected void updateNewIndex(Long jobId, String newIndex) {
        RagReindexJob job = ragReindexJobRepository.findById(jobId).orElseThrow();
        job.setNewIndex(newIndex);
        ragReindexJobRepository.save(job);
    }

    @Transactional
    protected void applyChunkSizeToConfig(Integer chunkSize) {
        RagConfig cfg = ragConfigRepository.findById(1L).orElseGet(() -> {
            RagConfig c = new RagConfig();
            c.setId(1L);
            return c;
        });
        cfg.setChunkSize(chunkSize);
        ragConfigRepository.save(cfg);
        // 刷新内存缓存
        ragConfigService.init();
    }

    public RagReindexJob getLatestJob() {
        return ragReindexJobRepository.findFirstByOrderByCreatedAtDesc().orElse(null);
    }

    public List<RagReindexJob> listRecentJobs() {
        LocalDateTime after = LocalDateTime.now().minusDays(RETENTION_DAYS);
        return ragReindexJobRepository.findByCreatedAtAfterOrderByCreatedAtDesc(after);
    }

    @Scheduled(cron = "0 40 3 * * *")
    @Transactional
    public void cleanupOldJobs() {
        LocalDateTime before = LocalDateTime.now().minusDays(RETENTION_DAYS);
        long deleted = ragReindexJobRepository.deleteByCreatedAtBefore(before);
        if (deleted > 0) {
            log.info("重建索引任务清理完成: deleted_jobs={}, before={}", deleted, before);
        }
    }

}
