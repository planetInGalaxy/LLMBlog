package com.lingdang.blog.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lingdang.blog.config.LlmConfig;
import com.lingdang.blog.dto.llm.*;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * LLM 服务（抽象 OpenAI 协议）
 */
@Slf4j
@Service
public class LlmService {
    
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    
    @Autowired
    private LlmConfig llmConfig;
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    private OkHttpClient httpClient;
    
    /**
     * 初始化 HTTP 客户端
     */
    private OkHttpClient getHttpClient() {
        if (httpClient == null) {
            httpClient = new OkHttpClient.Builder()
                .connectTimeout(llmConfig.getConnectTimeout(), TimeUnit.MILLISECONDS)
                .readTimeout(llmConfig.getReadTimeout(), TimeUnit.MILLISECONDS)
                .build();
        }
        return httpClient;
    }
    
    /**
     * 生成单个文本的 embedding
     */
    public float[] generateEmbedding(String text) throws IOException {
        List<float[]> embeddings = generateEmbeddings(List.of(text));
        return embeddings.isEmpty() ? null : embeddings.get(0);
    }
    
    /**
     * 批量生成 embeddings
     */
    public List<float[]> generateEmbeddings(List<String> texts) throws IOException {
        // 判断是否使用 Ollama
        if (llmConfig.isUseOllamaEmbedding()) {
            return generateEmbeddingsWithOllama(texts);
        }
        
        // 使用常规 OpenAI 协议
        EmbeddingRequest request = new EmbeddingRequest(
            llmConfig.getEmbeddingModel(),
            texts,
            "float"
        );
        
        String requestBody = objectMapper.writeValueAsString(request);
        
        Request httpRequest = new Request.Builder()
            .url(llmConfig.getBaseUrl() + "/embeddings")
            .header("Authorization", "Bearer " + llmConfig.getApiKey())
            .header("Content-Type", "application/json")
            .post(RequestBody.create(requestBody, JSON))
            .build();
        
        try (Response response = getHttpClient().newCall(httpRequest).execute()) {
            if (!response.isSuccessful()) {
                String errorBody = response.body() != null ? response.body().string() : "Unknown error";
                throw new IOException("Embedding API failed: " + response.code() + " - " + errorBody);
            }
            
            String responseBody = response.body().string();
            EmbeddingResponse embeddingResponse = objectMapper.readValue(responseBody, EmbeddingResponse.class);
            
            return embeddingResponse.getData().stream()
                .map(data -> {
                    List<Float> embedding = data.getEmbedding();
                    float[] result = new float[embedding.size()];
                    for (int i = 0; i < embedding.size(); i++) {
                        result[i] = embedding.get(i);
                    }
                    return result;
                })
                .collect(Collectors.toList());
        }
    }
    
    /**
     * 使用 Ollama 生成 embeddings（兼容 OpenAI 格式）
     */
    private List<float[]> generateEmbeddingsWithOllama(List<String> texts) throws IOException {
        log.info("使用 Ollama 生成 embeddings: {} 个文本", texts.size());
        
        // Ollama 使用 OpenAI 兼容 API，可以直接调用 /v1/embeddings
        EmbeddingRequest request = new EmbeddingRequest(
            llmConfig.getEmbeddingModel(),
            texts,
            "float"
        );
        
        String requestBody = objectMapper.writeValueAsString(request);
        
        Request httpRequest = new Request.Builder()
            .url(llmConfig.getOllamaBaseUrl() + "/v1/embeddings")
            .header("Content-Type", "application/json")
            // Ollama 不需要 Authorization
            .post(RequestBody.create(requestBody, JSON))
            .build();
        
        try (Response response = getHttpClient().newCall(httpRequest).execute()) {
            if (!response.isSuccessful()) {
                String errorBody = response.body() != null ? response.body().string() : "Unknown error";
                log.error("Ollama Embedding API 失败: {} - {}", response.code(), errorBody);
                throw new IOException("Ollama Embedding API failed: " + response.code() + " - " + errorBody);
            }
            
            String responseBody = response.body().string();
            EmbeddingResponse embeddingResponse = objectMapper.readValue(responseBody, EmbeddingResponse.class);
            
            return embeddingResponse.getData().stream()
                .map(data -> {
                    List<Float> embedding = data.getEmbedding();
                    float[] result = new float[embedding.size()];
                    for (int i = 0; i < embedding.size(); i++) {
                        result[i] = embedding.get(i);
                    }
                    return result;
                })
                .collect(Collectors.toList());
        }
    }
    
    /**
     * Chat Completion
     */
    public String chatCompletion(List<ChatCompletionRequest.ChatMessage> messages) throws IOException {
        return chatCompletion(messages, null);
    }
    
    /**
     * Chat Completion（带 maxTokens）
     */
    public String chatCompletion(List<ChatCompletionRequest.ChatMessage> messages, Integer maxTokens) throws IOException {
        ChatCompletionRequest request = new ChatCompletionRequest(
            llmConfig.getChatModel(),
            messages,
            0.7,
            maxTokens
        );
        
        String requestBody = objectMapper.writeValueAsString(request);
        
        Request httpRequest = new Request.Builder()
            .url(llmConfig.getBaseUrl() + "/chat/completions")
            .header("Authorization", "Bearer " + llmConfig.getApiKey())
            .header("Content-Type", "application/json")
            .post(RequestBody.create(requestBody, JSON))
            .build();
        
        try (Response response = getHttpClient().newCall(httpRequest).execute()) {
            if (!response.isSuccessful()) {
                String errorBody = response.body() != null ? response.body().string() : "Unknown error";
                throw new IOException("ChatCompletion API failed: " + response.code() + " - " + errorBody);
            }
            
            String responseBody = response.body().string();
            ChatCompletionResponse chatResponse = objectMapper.readValue(responseBody, ChatCompletionResponse.class);
            
            if (chatResponse.getChoices() == null || chatResponse.getChoices().isEmpty()) {
                throw new IOException("No choices in ChatCompletion response");
            }
            
            return chatResponse.getChoices().get(0).getMessage().getContent();
        }
    }
    
    /**
     * Chat Completion（返回完整响应，包含 usage）
     */
    public ChatCompletionResponse chatCompletionWithUsage(List<ChatCompletionRequest.ChatMessage> messages, Integer maxTokens) throws IOException {
        ChatCompletionRequest request = new ChatCompletionRequest(
            llmConfig.getChatModel(),
            messages,
            0.7,
            maxTokens
        );
        
        String requestBody = objectMapper.writeValueAsString(request);
        
        Request httpRequest = new Request.Builder()
            .url(llmConfig.getBaseUrl() + "/chat/completions")
            .header("Authorization", "Bearer " + llmConfig.getApiKey())
            .header("Content-Type", "application/json")
            .post(RequestBody.create(requestBody, JSON))
            .build();
        
        try (Response response = getHttpClient().newCall(httpRequest).execute()) {
            if (!response.isSuccessful()) {
                String errorBody = response.body() != null ? response.body().string() : "Unknown error";
                throw new IOException("ChatCompletion API failed: " + response.code() + " - " + errorBody);
            }
            
            String responseBody = response.body().string();
            return objectMapper.readValue(responseBody, ChatCompletionResponse.class);
        }
    }
    
    /**
     * Chat Completion 流式输出
     */
    public void chatCompletionStream(List<ChatCompletionRequest.ChatMessage> messages, Integer maxTokens, StreamCallback callback) throws IOException {
        log.info("开始 LLM 流式请求: model={}, messages_count={}, max_tokens={}", 
            llmConfig.getChatModel(), messages.size(), maxTokens);
        
        ChatCompletionRequest request = new ChatCompletionRequest(
            llmConfig.getChatModel(),
            messages,
            0.7,
            maxTokens
        );
        
        // 添加 stream 参数
        String requestBody = objectMapper.writeValueAsString(request).replace("}", ",\"stream\":true}");
        log.debug("LLM 请求体: {}", requestBody.length() > 500 ? requestBody.substring(0, 500) + "..." : requestBody);
        
        Request httpRequest = new Request.Builder()
            .url(llmConfig.getBaseUrl() + "/chat/completions")
            .header("Authorization", "Bearer " + llmConfig.getApiKey())
            .header("Content-Type", "application/json")
            .post(RequestBody.create(requestBody, JSON))
            .build();
        
        try (Response response = getHttpClient().newCall(httpRequest).execute()) {
            if (!response.isSuccessful()) {
                String errorBody = response.body() != null ? response.body().string() : "Unknown";
                log.error("LLM 流式请求失败: code={}, error={}", response.code(), errorBody);
                throw new IOException("LLM 流式请求失败: " + response.code() + " - " + errorBody);
            }
            
            log.info("LLM 流式响应开始接收");
            
            // 逐行读取流式响应
            java.io.BufferedReader reader = new java.io.BufferedReader(
                new java.io.InputStreamReader(response.body().byteStream())
            );
            
            int chunkCount = 0;
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("data: ")) {
                    String data = line.substring(6).trim();
                    if ("[DONE]".equals(data)) {
                        log.info("LLM 流式响应完成: 共接收 {} 个 chunks", chunkCount);
                        break;
                    }
                    try {
                        var chunk = objectMapper.readTree(data);
                        var choices = chunk.get("choices");
                        if (choices != null && choices.size() > 0) {
                            var delta = choices.get(0).get("delta");
                            if (delta != null && delta.has("content")) {
                                String content = delta.get("content").asText();
                                chunkCount++;
                                if (chunkCount <= 5 || chunkCount % 50 == 0) {
                                    log.debug("接收 LLM chunk #{}: '{}'", chunkCount, content);
                                }
                                callback.onChunk(content);
                            }
                        }
                    } catch (Exception e) {
                        log.warn("解析流式响应失败: data={}", data, e);
                    }
                }
            }
        } catch (IOException e) {
            log.error("LLM 流式请求异常", e);
            throw e;
        }
    }
    
    /**
     * 流式回调接口
     */
    @FunctionalInterface
    public interface StreamCallback {
        void onChunk(String chunk);
    }
}
