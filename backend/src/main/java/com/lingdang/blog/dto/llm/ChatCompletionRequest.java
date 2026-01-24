package com.lingdang.blog.dto.llm;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * ChatCompletion API 请求
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ChatCompletionRequest {
    
    /**
     * 模型名称
     */
    private String model;
    
    /**
     * 消息列表
     */
    private List<ChatMessage> messages;
    
    /**
     * 温度参数
     */
    private Double temperature = 0.7;
    
    /**
     * 最大 token 数
     */
    @JsonProperty("max_tokens")
    private Integer maxTokens;
    
    /**
     * 深度思考配置（豆包专用）
     */
    private ThinkingConfig thinking;
    
    /**
     * 深度思考配置
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ThinkingConfig {
        /**
         * 思考类型：enabled、disabled、auto
         */
        private String type;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ChatMessage {
        private String role;
        private String content;
    }
}
