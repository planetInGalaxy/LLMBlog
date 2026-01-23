package com.lingdang.blog.dto.assistant;

import lombok.Data;

import java.util.List;

/**
 * Assistant 查询响应
 */
@Data
public class AssistantResponse {
    
    /**
     * 回答内容
     */
    private String answer;
    
    /**
     * 引用列表
     */
    private List<Citation> citations;
    
    /**
     * 查询 ID
     */
    private String queryId;
    
    /**
     * 响应延迟（毫秒）
     */
    private Integer latencyMs;
    
    @Data
    public static class Citation {
        /**
         * 文章标题
         */
        private String title;
        
        /**
         * 跳转 URL
         */
        private String url;
        
        /**
         * 引用片段
         */
        private String quote;
        
        /**
         * Chunk ID
         */
        private String chunkId;
        
        /**
         * 相关度分数
         */
        private Double score;
    }
}
