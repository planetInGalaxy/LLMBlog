package com.lingdang.blog.dto.llm;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

/**
 * Embedding API 响应
 */
@Data
public class EmbeddingResponse {
    
    private String object;
    private List<EmbeddingData> data;
    private String model;
    private Usage usage;
    
    @Data
    public static class EmbeddingData {
        private String object;
        private Integer index;
        private List<Float> embedding;
    }
    
    @Data
    public static class Usage {
        @JsonProperty("prompt_tokens")
        private Integer promptTokens;
        @JsonProperty("total_tokens")
        private Integer totalTokens;
    }
}
