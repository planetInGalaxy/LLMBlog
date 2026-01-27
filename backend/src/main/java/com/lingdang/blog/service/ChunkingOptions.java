package com.lingdang.blog.service;

import lombok.Data;

/**
 * Chunk 切分参数（使用 ChunkService 内部的“估算 token”口径：字符数/4）。
 */
@Data
public class ChunkingOptions {
    /** 最小 chunk size（超过 max 才会分裂；分裂时尽量保证每段 >= min） */
    private int minTokens;
    /** 目标最大 chunk size */
    private int maxTokens;
    /** 相邻 chunk 的重叠（估算 token） */
    private int overlapTokens;

    public static ChunkingOptions of(int minTokens, int maxTokens, int overlapTokens) {
        ChunkingOptions o = new ChunkingOptions();
        o.setMinTokens(minTokens);
        o.setMaxTokens(maxTokens);
        o.setOverlapTokens(overlapTokens);
        return o;
    }
}
