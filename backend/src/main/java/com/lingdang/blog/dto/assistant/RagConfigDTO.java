package com.lingdang.blog.dto.assistant;

import lombok.Data;

/**
 * RAG 配置
 */
@Data
public class RagConfigDTO {
    /**
     * 检索结果数量
     */
    private Integer topK;

    /**
     * 最小相关度阈值（0~1）
     */
    private Double minScore;

    /**
     * Chunk 大小（展示用）
     */
    private Integer chunkSize;

    /**
     * 是否返回引用
     */
    private Boolean returnCitations;

    /**
     * 混合检索权重：向量相似度权重（0~100）
     */
    private Integer vectorWeight;

    /**
     * 混合检索权重：BM25 权重（0~100）
     */
    private Integer bm25Weight;
}
