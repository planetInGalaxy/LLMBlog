package com.lingdang.blog.service;

import com.lingdang.blog.dto.assistant.RagConfigDTO;
import com.lingdang.blog.model.RagConfig;
import com.lingdang.blog.repository.RagConfigRepository;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * RAG 配置服务（MySQL 持久化）
 *
 * 说明：
 * - 使用单行配置（id=1）进行存储。
 * - Studio 修改后立即生效，并且重启后保留。
 */
@Slf4j
@Service
public class RagConfigService {
    private static final long SINGLETON_ID = 1L;

    private static final int DEFAULT_TOP_K = 5;
    private static final double DEFAULT_MIN_SCORE = 0.0;
    private static final int DEFAULT_CHUNK_SIZE = 900;
    private static final boolean DEFAULT_RETURN_CITATIONS = true;

    @Autowired
    private RagConfigRepository ragConfigRepository;

    private final Object lock = new Object();
    private RagConfigDTO current;

    @PostConstruct
    public void init() {
        synchronized (lock) {
            RagConfig entity = ensureEntity();
            current = toDTO(entity);
            log.info("RAG 配置已加载: topK={}, minScore={}, chunkSize={}, returnCitations={}",
                current.getTopK(), current.getMinScore(), current.getChunkSize(), current.getReturnCitations());
        }
    }

    public RagConfigDTO getConfig() {
        synchronized (lock) {
            // current 作为缓存即可；更新时会同步写库并刷新
            return copy(current);
        }
    }

    public RagConfigDTO updateConfig(RagConfigDTO update) {
        if (update == null) {
            throw new IllegalArgumentException("配置不能为空");
        }

        synchronized (lock) {
            validate(update);

            RagConfig entity = ensureEntity();

            if (update.getTopK() != null) {
                entity.setTopK(update.getTopK());
            }
            if (update.getMinScore() != null) {
                entity.setMinScore(update.getMinScore());
            }
            if (update.getReturnCitations() != null) {
                entity.setReturnCitations(update.getReturnCitations());
            }

            if (update.getChunkSize() != null && !update.getChunkSize().equals(entity.getChunkSize())) {
                log.info("chunkSize 变更仅展示用，需重建索引后生效: {} -> {}",
                    entity.getChunkSize(), update.getChunkSize());
                entity.setChunkSize(update.getChunkSize());
            }

            RagConfig saved = ragConfigRepository.save(entity);
            current = toDTO(saved);
            return copy(current);
        }
    }

    private void validate(RagConfigDTO update) {
        if (update.getTopK() != null) {
            int topK = update.getTopK();
            if (topK < 1 || topK > 50) {
                throw new IllegalArgumentException("topK 需在 1 ~ 50 之间");
            }
        }

        if (update.getMinScore() != null) {
            double minScore = update.getMinScore();
            if (minScore < 0 || minScore > 1) {
                throw new IllegalArgumentException("minScore 需在 0 ~ 1 之间");
            }
        }
    }

    private RagConfig ensureEntity() {
        Optional<RagConfig> existing = ragConfigRepository.findById(SINGLETON_ID);
        if (existing.isPresent()) {
            return existing.get();
        }

        RagConfig created = new RagConfig();
        created.setId(SINGLETON_ID);
        created.setTopK(DEFAULT_TOP_K);
        created.setMinScore(DEFAULT_MIN_SCORE);
        created.setChunkSize(DEFAULT_CHUNK_SIZE);
        created.setReturnCitations(DEFAULT_RETURN_CITATIONS);

        return ragConfigRepository.save(created);
    }

    private RagConfigDTO toDTO(RagConfig entity) {
        RagConfigDTO dto = new RagConfigDTO();
        dto.setTopK(entity.getTopK() != null ? entity.getTopK() : DEFAULT_TOP_K);
        dto.setMinScore(entity.getMinScore() != null ? entity.getMinScore() : DEFAULT_MIN_SCORE);
        dto.setChunkSize(entity.getChunkSize() != null ? entity.getChunkSize() : DEFAULT_CHUNK_SIZE);
        dto.setReturnCitations(Boolean.TRUE.equals(entity.getReturnCitations()));
        return dto;
    }

    private RagConfigDTO copy(RagConfigDTO source) {
        RagConfigDTO copy = new RagConfigDTO();
        copy.setTopK(source.getTopK());
        copy.setMinScore(source.getMinScore());
        copy.setChunkSize(source.getChunkSize());
        copy.setReturnCitations(source.getReturnCitations());
        return copy;
    }
}

