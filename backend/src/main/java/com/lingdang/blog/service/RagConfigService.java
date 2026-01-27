package com.lingdang.blog.service;

import com.lingdang.blog.dto.assistant.RagConfigDTO;
import com.lingdang.blog.model.RagConfig;
import com.lingdang.blog.model.RagReindexJob;
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
    private static final int DEFAULT_VECTOR_WEIGHT = 70;
    private static final int DEFAULT_BM25_WEIGHT = 30;
    private static final double DEFAULT_BM25_MAX = 15.0;

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
            // 为了避免异步任务更新 DB 后缓存不一致，这里每次读取都用 DB 刷新一次缓存。
            try {
                RagConfig entity = ensureEntity();
                current = toDTO(entity);
            } catch (Exception e) {
                log.warn("读取 RAG 配置失败，将返回缓存值: {}", e.getMessage());
            }
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
            if (update.getVectorWeight() != null) {
                entity.setVectorWeight(update.getVectorWeight());
            }
            if (update.getBm25Weight() != null) {
                entity.setBm25Weight(update.getBm25Weight());
            }
            if (update.getBm25Max() != null) {
                entity.setBm25Max(update.getBm25Max());
            }

            if (update.getChunkSize() != null) {
                entity.setChunkSize(update.getChunkSize());
            }

            RagConfig saved = ragConfigRepository.save(entity);
            current = toDTO(saved);
            return copy(current);
        }
    }

    // (removed) RagReindexJobService dependency to avoid circular reference

    /**
     * 更新 RAG 配置：
     * - 立即生效的参数：topK/minScore/returnCitations -> 直接落库
     * - 需要重建索引的参数：chunkSize -> 自动触发全量重建索引
     *
     * 要求：重建成功才真正落库；失败则保持旧配置 + 旧索引，并返回错误。
     */
    public RagConfigDTO updateConfigAndReindexIfNeeded(RagConfigDTO update) {
        if (update == null) {
            throw new IllegalArgumentException("配置不能为空");
        }

        synchronized (lock) {
            validate(update);

            RagConfig entity = ensureEntity();
            RagConfigDTO before = toDTO(entity);

            boolean chunkSizeChanged = update.getChunkSize() != null
                && !update.getChunkSize().equals(before.getChunkSize());

            log.info("处理 rag-config 更新: before(chunkSize={}), requested(chunkSize={}), chunkSizeChanged={}",
                before.getChunkSize(), update.getChunkSize(), chunkSizeChanged);

            // 1) 对立即生效参数，先更新内存/DB
            RagConfigDTO next = copy(before);
            if (update.getTopK() != null) next.setTopK(update.getTopK());
            if (update.getMinScore() != null) next.setMinScore(update.getMinScore());
            if (update.getReturnCitations() != null) next.setReturnCitations(update.getReturnCitations());
            if (update.getVectorWeight() != null) next.setVectorWeight(update.getVectorWeight());
            if (update.getBm25Weight() != null) next.setBm25Weight(update.getBm25Weight());
            if (update.getBm25Max() != null) next.setBm25Max(update.getBm25Max());

            // 2) chunkSize 是否变化由 Controller 决定是否提交异步重建任务。
            //    这里不再依赖 RagReindexJobService，避免循环依赖。

            // 3) 立即生效参数可直接落库 + 更新缓存
            entity.setTopK(next.getTopK());
            entity.setMinScore(next.getMinScore());
            entity.setReturnCitations(next.getReturnCitations());
            entity.setVectorWeight(next.getVectorWeight());
            entity.setBm25Weight(next.getBm25Weight());
            entity.setBm25Max(next.getBm25Max());

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

        if (update.getChunkSize() != null) {
            int chunkSize = update.getChunkSize();
            // 使用估算 token 口径，给一个保守可用范围
            if (chunkSize < 50 || chunkSize > 2000) {
                throw new IllegalArgumentException("chunkSize 建议在 50 ~ 2000 之间");
            }
        }

        if (update.getVectorWeight() != null || update.getBm25Weight() != null) {
            int vectorW = update.getVectorWeight() != null ? update.getVectorWeight() : -1;
            int bm25W = update.getBm25Weight() != null ? update.getBm25Weight() : -1;
            if ((vectorW != -1 && (vectorW < 0 || vectorW > 100)) || (bm25W != -1 && (bm25W < 0 || bm25W > 100))) {
                throw new IllegalArgumentException("权重需在 0 ~ 100 之间");
            }
            if (vectorW != -1 && bm25W != -1 && vectorW + bm25W != 100) {
                throw new IllegalArgumentException("vectorWeight + bm25Weight 必须等于 100");
            }
        }

        if (update.getBm25Max() != null) {
            double bm25Max = update.getBm25Max();
            if (bm25Max <= 0 || bm25Max > 1000) {
                throw new IllegalArgumentException("bm25Max 需在 (0, 1000] 之间");
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
        created.setVectorWeight(DEFAULT_VECTOR_WEIGHT);
        created.setBm25Weight(DEFAULT_BM25_WEIGHT);
        created.setBm25Max(DEFAULT_BM25_MAX);

        return ragConfigRepository.save(created);
    }

    private RagConfigDTO toDTO(RagConfig entity) {
        RagConfigDTO dto = new RagConfigDTO();
        dto.setTopK(entity.getTopK() != null ? entity.getTopK() : DEFAULT_TOP_K);
        dto.setMinScore(entity.getMinScore() != null ? entity.getMinScore() : DEFAULT_MIN_SCORE);
        dto.setChunkSize(entity.getChunkSize() != null ? entity.getChunkSize() : DEFAULT_CHUNK_SIZE);
        dto.setReturnCitations(Boolean.TRUE.equals(entity.getReturnCitations()));

        Integer vectorW = entity.getVectorWeight();
        Integer bm25W = entity.getBm25Weight();
        Double bm25Max = entity.getBm25Max();
        if (vectorW == null && bm25W == null) {
            vectorW = DEFAULT_VECTOR_WEIGHT;
            bm25W = DEFAULT_BM25_WEIGHT;
        } else if (vectorW == null) {
            vectorW = Math.max(0, 100 - bm25W);
        } else if (bm25W == null) {
            bm25W = Math.max(0, 100 - vectorW);
        }
        dto.setVectorWeight(vectorW);
        dto.setBm25Weight(bm25W);
        dto.setBm25Max(bm25Max != null ? bm25Max : DEFAULT_BM25_MAX);

        return dto;
    }

    /**
     * 将 rag-config 的 chunkSize 转换为实际切分参数。
     * chunkSize 的口径与 ChunkService 内部 estimateTokenCount 保持一致（字符数/4）。
     */
    public ChunkingOptions getChunkingOptions() {
        RagConfigDTO cfg = getConfig();
        int max = cfg.getChunkSize() != null ? cfg.getChunkSize() : DEFAULT_CHUNK_SIZE;
        max = Math.max(50, max);

        // 经验值：min 约为 max 的 2/3，overlap 约为 max 的 10%
        // 关键：必须保证 min <= max，否则切分逻辑会生成远超 max 的 chunk（进而导致 embedding 超上下文长度）
        int min = (int) Math.round(max * 0.67);
        min = Math.max(20, min);
        min = Math.min(min, max);

        int overlap = (int) Math.round(max * 0.10);
        overlap = Math.max(0, overlap);
        overlap = Math.min(overlap, Math.max(0, max - 1));

        return ChunkingOptions.of(min, max, overlap);
    }

    private RagConfigDTO copy(RagConfigDTO source) {
        RagConfigDTO copy = new RagConfigDTO();
        copy.setTopK(source.getTopK());
        copy.setMinScore(source.getMinScore());
        copy.setChunkSize(source.getChunkSize());
        copy.setReturnCitations(source.getReturnCitations());
        copy.setVectorWeight(source.getVectorWeight());
        copy.setBm25Weight(source.getBm25Weight());
        copy.setBm25Max(source.getBm25Max());
        return copy;
    }
}

