package com.lingdang.blog.service;

import com.lingdang.blog.dto.assistant.RagConfigDTO;
import com.lingdang.blog.model.RagQueryHit;
import com.lingdang.blog.model.RagQueryLog;
import com.lingdang.blog.repository.RagQueryHitRepository;
import com.lingdang.blog.repository.RagQueryLogRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * RAG 可观测性：记录查询日志/命中，并做定期清理（保留 7 天）
 */
@Slf4j
@Service
public class RagObservabilityService {

    private static final int RETENTION_DAYS = 7;

    @Autowired
    private RagQueryLogRepository ragQueryLogRepository;

    @Autowired
    private RagQueryHitRepository ragQueryHitRepository;

    @Transactional
    public void upsertQueryLog(RagQueryLog logEntity) {
        // request_id 唯一，save 会在第一次 insert；如果重复可以先查再更新
        ragQueryLogRepository.findByRequestId(logEntity.getRequestId()).ifPresent(existing -> logEntity.setId(existing.getId()));
        ragQueryLogRepository.save(logEntity);
    }

    @Transactional
    public void replaceHits(String requestId, List<RagQueryHit> hits) {
        // 简化：不做按 requestId 删除（避免额外 SQL），因为 requestId 唯一且基本只写一次。
        // 如果未来出现重复写，可补充 deleteByRequestId。
        ragQueryHitRepository.saveAll(hits);
    }

    public RagQueryLog buildBaseLog(String requestId, String clientIp, String question, RagConfigDTO cfg) {
        RagQueryLog l = new RagQueryLog();
        l.setRequestId(requestId);
        l.setClientIp(clientIp);
        l.setQuestion(question);
        if (cfg != null) {
            l.setTopK(cfg.getTopK());
            l.setMinScore(cfg.getMinScore());
            l.setChunkSize(cfg.getChunkSize());
            l.setReturnCitations(cfg.getReturnCitations());
        }
        return l;
    }

    /**
     * 每天凌晨清理 7 天前数据
     */
    @Scheduled(cron = "0 30 3 * * *")
    @Transactional
    public void cleanupOldLogs() {
        LocalDateTime before = LocalDateTime.now().minusDays(RETENTION_DAYS);
        long hitDeleted = ragQueryHitRepository.deleteByCreatedAtBefore(before);
        long logDeleted = ragQueryLogRepository.deleteByCreatedAtBefore(before);
        if (hitDeleted > 0 || logDeleted > 0) {
            log.info("RAG 日志清理完成: deleted_logs={}, deleted_hits={}, before={}", logDeleted, hitDeleted, before);
        }
    }
}
