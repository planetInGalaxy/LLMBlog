package com.lingdang.blog.service;

import com.lingdang.blog.dto.assistant.RagConfigDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * RAG 配置服务（内存存储）
 */
@Slf4j
@Service
public class RagConfigService {
    private static final int DEFAULT_TOP_K = 5;
    private static final double DEFAULT_MIN_SCORE = 0.0;
    private static final int DEFAULT_CHUNK_SIZE = 900;
    private static final boolean DEFAULT_RETURN_CITATIONS = true;

    private final Object lock = new Object();
    private RagConfigDTO current = defaultConfig();

    public RagConfigDTO getConfig() {
        synchronized (lock) {
            return copy(current);
        }
    }

    public RagConfigDTO updateConfig(RagConfigDTO update) {
        if (update == null) {
            throw new IllegalArgumentException("配置不能为空");
        }

        synchronized (lock) {
            validate(update);

            RagConfigDTO next = copy(current);

            if (update.getTopK() != null) {
                next.setTopK(update.getTopK());
            }
            if (update.getMinScore() != null) {
                next.setMinScore(update.getMinScore());
            }
            if (update.getReturnCitations() != null) {
                next.setReturnCitations(update.getReturnCitations());
            }

            if (update.getChunkSize() != null && !update.getChunkSize().equals(current.getChunkSize())) {
                log.info("chunkSize 变更仅展示用，需重建索引后生效: {} -> {}",
                    current.getChunkSize(), update.getChunkSize());
            }

            current = next;
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

    private RagConfigDTO defaultConfig() {
        RagConfigDTO config = new RagConfigDTO();
        config.setTopK(DEFAULT_TOP_K);
        config.setMinScore(DEFAULT_MIN_SCORE);
        config.setChunkSize(DEFAULT_CHUNK_SIZE);
        config.setReturnCitations(DEFAULT_RETURN_CITATIONS);
        return config;
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
