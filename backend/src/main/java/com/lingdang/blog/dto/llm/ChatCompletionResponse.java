package com.lingdang.blog.dto.llm;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

/**
 * ChatCompletion API 响应
 */
@Data
public class ChatCompletionResponse {
    
    private String id;
    private String object;
    private Long created;
    private String model;
    private List<Choice> choices;
    private Usage usage;
    
    @Data
    public static class Choice {
        private Integer index;
        private ChatCompletionRequest.ChatMessage message;
        @JsonProperty("finish_reason")
        private String finishReason;
    }
    
    @Data
    public static class Usage {
        @JsonProperty("prompt_tokens")
        private Integer promptTokens;
        @JsonProperty("completion_tokens")
        private Integer completionTokens;
        @JsonProperty("total_tokens")
        private Integer totalTokens;
    }
}
