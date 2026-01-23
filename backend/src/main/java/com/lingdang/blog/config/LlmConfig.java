package com.lingdang.blog.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * LLM 配置类
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "llm")
public class LlmConfig {
    
    /**
     * LLM API Base URL
     */
    private String baseUrl = "https://api.openai.com/v1";
    
    /**
     * API Key
     */
    private String apiKey;
    
    /**
     * Embedding 模型
     */
    private String embeddingModel = "text-embedding-3-small";
    
    /**
     * Chat 模型
     */
    private String chatModel = "gpt-4o-mini";
    
    /**
     * Ollama Base URL
     */
    private String ollamaBaseUrl = "http://ollama:11434";
    
    /**
     * 是否使用 Ollama 进行 Embedding
     */
    private boolean useOllamaEmbedding = false;
    
    /**
     * 连接超时（毫秒）
     */
    private long connectTimeout = 30000;
    
    /**
     * 读取超时（毫秒）
     */
    private long readTimeout = 60000;
}
