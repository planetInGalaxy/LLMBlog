package com.lingdang.blog.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lingdang.blog.dto.assistant.AssistantRequest;
import com.lingdang.blog.dto.assistant.AssistantResponse;
import com.lingdang.blog.dto.llm.ChatCompletionRequest;
import com.lingdang.blog.dto.llm.ChatCompletionResponse;
import com.lingdang.blog.model.AssistantLog;
import com.lingdang.blog.model.ChunkDocument;
import com.lingdang.blog.repository.AssistantLogRepository;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * RAG 检索服务
 */
@Slf4j
@Service
public class RagService {
    
    @Autowired
    private ElasticsearchClient esClient;
    
    @Autowired
    private LlmService llmService;
    
    @Autowired
    private AssistantLogRepository assistantLogRepository;
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    // 相关度阈值：只有 >= 80% 的文章才会被引用
    private static final double RELEVANCE_THRESHOLD = 0.8;
    
    // 最多引用文章数：前 3 篇
    private static final int MAX_CITATIONS = 3;
    
    // System Prompt
    private static final String SYSTEM_PROMPT_WITH_ARTICLES = """
        你是铃铛师兄大模型博客的智能助手。你的任务是基于提供的文章片段回答用户的问题。
        
        **重要规则**：
        1. 只基于提供的文章片段回答，不要编造信息
        2. 如果文章中没有相关信息，明确告知用户"文章库中未找到相关依据"
        3. 回答要准确、简洁、专业
        4. 在回答中标注引用来源（使用 [1]、[2] 等标记）
        5. 保持中文回答
        """;
    
    private static final String SYSTEM_PROMPT_FLEXIBLE = """
        你是铃铛师兄大模型博客的智能助手，也是大模型和AI领域的专家。
        
        **你的职责**：
        1. 如果提供了相关文章片段，优先基于文章内容回答，并标注引用来源
        2. 如果没有相关文章或文章信息不足，可以基于你的专业知识回答
        3. 回答要准确、专业、有深度，特别在大模型、AI技术、机器学习等领域
        4. 保持中文回答，语言友好易懂
        5. 如果不确定答案，诚实告知用户
        
        **你的专长领域**：
        - 大语言模型（LLM）架构、训练、推理
        - AI 技术应用（RAG、Agent、Fine-tuning 等）
        - 机器学习算法和深度学习
        - 自然语言处理（NLP）
        - 技术博客写作和知识分享
        """;
    
    /**
     * 查询 Assistant
     */
    public AssistantResponse query(AssistantRequest request, String clientIp) {
        long startTime = System.currentTimeMillis();
        String requestId = UUID.randomUUID().toString();
        
        try {
            // 1. 生成查询向量
            float[] queryEmbedding = llmService.generateEmbedding(request.getQuestion());
            
            // 2. 混合检索（向量 + BM25）
            List<RetrievalResult> results = hybridSearch(request.getQuestion(), queryEmbedding);
            
            // 3. 过滤高相关度文章（>= 80%）
            List<RetrievalResult> highRelevanceResults = filterHighRelevanceResults(results);
            log.info("相关度过滤: 检索到 {} 条结果, 高相关度（>=80%）{} 条", 
                results != null ? results.size() : 0, highRelevanceResults.size());
            
            // 4. 判断模式和是否有高相关度检索结果
            boolean hasArticles = !highRelevanceResults.isEmpty();
            boolean isFlexibleMode = "FLEXIBLE".equalsIgnoreCase(request.getMode());
            
            // 5. 构建消息列表（包含历史对话）
            List<ChatCompletionRequest.ChatMessage> messages = new ArrayList<>();
            
            // 添加系统提示
            String systemPrompt = (hasArticles || !isFlexibleMode) ? 
                SYSTEM_PROMPT_WITH_ARTICLES : SYSTEM_PROMPT_FLEXIBLE;
            messages.add(new ChatCompletionRequest.ChatMessage("system", systemPrompt));
            
            // 添加历史对话
            if (request.getHistory() != null && !request.getHistory().isEmpty()) {
                for (AssistantRequest.ChatMessage historyMsg : request.getHistory()) {
                    messages.add(new ChatCompletionRequest.ChatMessage(
                        historyMsg.getRole(), 
                        historyMsg.getContent()
                    ));
                }
            }
            
            // 添加当前问题
            String userPrompt = hasArticles ? 
                buildPrompt(request.getQuestion(), highRelevanceResults) : request.getQuestion();
            messages.add(new ChatCompletionRequest.ChatMessage("user", userPrompt));
            
            // 6. 调用 LLM
            String answer;
            List<AssistantResponse.Citation> citations;
            ChatCompletionResponse llmResponse;
            
            if (hasArticles || isFlexibleMode) {
                llmResponse = llmService.chatCompletionWithUsage(messages, 2048);
                answer = llmResponse.getChoices().get(0).getMessage().getContent();
                citations = hasArticles ? extractCitations(highRelevanceResults) : new ArrayList<>();
            } else {
                // ARTICLE_ONLY 模式且无高相关度文章：返回未找到
                answer = "抱歉，文章库中未找到与您问题高度相关的内容（相关度 < 80%）。您可以尝试换个问法，或者查看文章列表选择感兴趣的文章阅读。";
                citations = new ArrayList<>();
                llmResponse = null;
            }
            
            // 7. 构建响应
            AssistantResponse response = new AssistantResponse();
            response.setAnswer(answer);
            response.setCitations(citations);
            response.setQueryId(requestId);
            response.setLatencyMs((int) (System.currentTimeMillis() - startTime));
            
            // 8. 记录日志
            logQuery(requestId, clientIp, request, highRelevanceResults, llmResponse, response, true);
            
            return response;
            
        } catch (Exception e) {
            log.error("RAG 查询失败: request_id={}", requestId, e);
            
            // 记录失败日志
            logQuery(requestId, clientIp, request, Collections.emptyList(), null, null, false);
            
            throw new RuntimeException("查询失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 混合检索（向量 + BM25）
     */
    private List<RetrievalResult> hybridSearch(String question, float[] queryEmbedding) throws IOException {
        // 向量检索
        List<RetrievalResult> vectorResults = vectorSearch(queryEmbedding, 50);
        
        // BM25 检索
        List<RetrievalResult> bm25Results = bm25Search(question, 20);
        
        // 合并去重并重排序
        return mergeAndRerank(vectorResults, bm25Results, 6);
    }
    
    /**
     * 向量检索
     */
    private List<RetrievalResult> vectorSearch(float[] embedding, int topK) throws IOException {
        List<RetrievalResult> results = new ArrayList<>();
        
        try {
            // 先检查索引是否存在
            boolean indexExists = esClient.indices().exists(e -> e.index("lingdang_chunks_v1")).value();
            
            if (!indexExists) {
                log.warn("索引 lingdang_chunks_v1 不存在，跳过向量检索");
                return results;
            }
            
            // 检查索引是否有数据
            long count = esClient.count(c -> c.index("lingdang_chunks_v1")).count();
            if (count == 0) {
                log.warn("索引 lingdang_chunks_v1 为空，跳过向量检索");
                return results;
            }
            
            log.info("开始向量检索: index=lingdang_chunks_v1, topK={}, embedding_dim={}, doc_count={}", 
                topK, embedding.length, count);
            
            SearchResponse<ChunkDocument> response = esClient.search(s -> s
                .index("lingdang_chunks_v1")
                .knn(k -> k
                    .field("embedding")
                    .queryVector(floatArrayToList(embedding))
                    .k(topK)
                    .numCandidates(100)
                )
                .size(topK),
                ChunkDocument.class
            );
            
            log.info("向量检索成功: total_hits={}", response.hits().total().value());
            
            for (Hit<ChunkDocument> hit : response.hits().hits()) {
                ChunkDocument doc = hit.source();
                if (doc != null) {
                    RetrievalResult result = new RetrievalResult();
                    result.setChunkId(doc.getChunkId());
                    result.setArticleId(doc.getArticleId());
                    result.setSlug(doc.getSlug());
                    result.setTitle(doc.getTitle());
                    result.setAnchor(doc.getAnchor());
                    result.setChunkText(doc.getChunkText());
                    result.setVectorScore(hit.score() != null ? hit.score().doubleValue() : 0.0);
                    results.add(result);
                }
            }
            
            log.info("向量检索结果处理完成: result_count={}", results.size());
            
        } catch (Exception e) {
            log.error("❌ 向量检索失败，将返回空结果", e);
            log.error("❌ 异常类型: {}", e.getClass().getName());
            log.error("❌ 异常信息: {}", e.getMessage());
            if (e.getCause() != null) {
                log.error("❌ 根本原因: {}", e.getCause().getMessage());
            }
        }
        
        return results;
    }
    
    /**
     * BM25 检索
     */
    private List<RetrievalResult> bm25Search(String query, int topK) throws IOException {
        List<RetrievalResult> results = new ArrayList<>();
        
        try {
            // 先检查索引是否存在
            boolean indexExists = esClient.indices().exists(e -> e.index("lingdang_chunks_v1")).value();
            
            if (!indexExists) {
                log.warn("索引 lingdang_chunks_v1 不存在，跳过 BM25 检索");
                return results;
            }
            
            // 检查索引是否有数据
            long count = esClient.count(c -> c.index("lingdang_chunks_v1")).count();
            if (count == 0) {
                log.warn("索引 lingdang_chunks_v1 为空，跳过 BM25 检索");
                return results;
            }
            
            log.info("开始 BM25 检索: index=lingdang_chunks_v1, query='{}', topK={}, doc_count={}", 
                query, topK, count);
            
            SearchResponse<ChunkDocument> response = esClient.search(s -> s
                .index("lingdang_chunks_v1")
                .query(q -> q
                    .multiMatch(m -> m
                        .query(query)
                        .fields("title^2", "chunkText", "tags^1.5")
                    )
                )
                .size(topK),
                ChunkDocument.class
            );
            
            log.info("BM25 检索成功: total_hits={}", response.hits().total().value());
            
            for (Hit<ChunkDocument> hit : response.hits().hits()) {
                ChunkDocument doc = hit.source();
                if (doc != null) {
                    RetrievalResult result = new RetrievalResult();
                    result.setChunkId(doc.getChunkId());
                    result.setArticleId(doc.getArticleId());
                    result.setSlug(doc.getSlug());
                    result.setTitle(doc.getTitle());
                    result.setAnchor(doc.getAnchor());
                    result.setChunkText(doc.getChunkText());
                    result.setBm25Score(hit.score() != null ? hit.score().doubleValue() : 0.0);
                    results.add(result);
                }
            }
            
            log.info("BM25 检索结果处理完成: result_count={}", results.size());
            
        } catch (Exception e) {
            log.error("❌ BM25 检索失败，将返回空结果", e);
            log.error("❌ 异常类型: {}", e.getClass().getName());
            log.error("❌ 异常信息: {}", e.getMessage());
            if (e.getCause() != null) {
                log.error("❌ 根本原因: {}", e.getCause().getMessage());
            }
        }
        
        return results;
    }
    
    /**
     * 合并并重排序
     */
    private List<RetrievalResult> mergeAndRerank(List<RetrievalResult> vectorResults, 
                                                   List<RetrievalResult> bm25Results, 
                                                   int topK) {
        Map<String, RetrievalResult> merged = new HashMap<>();
        
        // 合并向量结果
        for (RetrievalResult result : vectorResults) {
            merged.put(result.getChunkId(), result);
        }
        
        // 合并 BM25 结果
        for (RetrievalResult result : bm25Results) {
            if (merged.containsKey(result.getChunkId())) {
                merged.get(result.getChunkId()).setBm25Score(result.getBm25Score());
            } else {
                merged.put(result.getChunkId(), result);
            }
        }
        
        // 找出最大分数用于归一化
        double maxVectorScore = merged.values().stream()
            .mapToDouble(RetrievalResult::getVectorScore)
            .max()
            .orElse(1.0);
        
        double maxBm25Score = merged.values().stream()
            .mapToDouble(RetrievalResult::getBm25Score)
            .max()
            .orElse(1.0);
        
        // 归一化并重排序：0.7 * normalized_vector + 0.3 * normalized_bm25
        // 最终分数范围：0.0 ~ 1.0
        return merged.values().stream()
            .peek(r -> {
                double normalizedVector = maxVectorScore > 0 ? r.getVectorScore() / maxVectorScore : 0.0;
                double normalizedBm25 = maxBm25Score > 0 ? r.getBm25Score() / maxBm25Score : 0.0;
                double finalScore = 0.7 * normalizedVector + 0.3 * normalizedBm25;
                r.setFinalScore(finalScore);
            })
            .sorted((a, b) -> Double.compare(b.getFinalScore(), a.getFinalScore()))
            .limit(topK)
            .collect(Collectors.toList());
    }
    
    /**
     * 构建 Prompt
     */
    private String buildPrompt(String question, List<RetrievalResult> results) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("**用户问题**：\n").append(question).append("\n\n");
        prompt.append("**相关文章片段**：\n\n");
        
        int index = 1;
        for (RetrievalResult result : results) {
            prompt.append(String.format("[%d] 《%s》\n", index, result.getTitle()));
            prompt.append(result.getChunkText()).append("\n\n");
            index++;
        }
        
        prompt.append("**请基于以上文章片段回答用户问题**。");
        
        return prompt.toString();
    }
    
    /**
     * 过滤高相关度文章（>= 80%，最多前 3 篇）
     */
    private List<RetrievalResult> filterHighRelevanceResults(List<RetrievalResult> results) {
        if (results == null || results.isEmpty()) {
            return new ArrayList<>();
        }
        
        return results.stream()
            .filter(r -> r.getFinalScore() >= RELEVANCE_THRESHOLD)
            .limit(MAX_CITATIONS)
            .collect(Collectors.toList());
    }
    
    /**
     * 提取引用（只提取已经过滤的高相关度文章）
     * 重要：按 articleId 去重，同一篇文章只显示一次
     */
    private List<AssistantResponse.Citation> extractCitations(List<RetrievalResult> results) {
        // 按 articleId 分组，每篇文章只取相关度最高的 chunk
        Map<Long, RetrievalResult> bestChunkPerArticle = new HashMap<>();
        
        for (RetrievalResult result : results) {
            Long articleId = result.getArticleId();
            if (!bestChunkPerArticle.containsKey(articleId) || 
                result.getFinalScore() > bestChunkPerArticle.get(articleId).getFinalScore()) {
                bestChunkPerArticle.put(articleId, result);
            }
        }
        
        // 转换为 Citation 列表（按分数降序）
        return bestChunkPerArticle.values().stream()
            .sorted((a, b) -> Double.compare(b.getFinalScore(), a.getFinalScore()))
            .map(result -> {
                AssistantResponse.Citation citation = new AssistantResponse.Citation();
                citation.setTitle(result.getTitle());
                citation.setUrl("/blog/" + result.getSlug() + 
                    (result.getAnchor() != null && !result.getAnchor().isEmpty() ? "#" + result.getAnchor() : ""));
                citation.setQuote(""); // 不显示文章内容，只显示标题
                citation.setChunkId(result.getChunkId());
                citation.setScore(result.getFinalScore()); // 已归一化到 0.0 ~ 1.0，且 >= 0.8
                return citation;
            })
            .collect(Collectors.toList());
    }
    
    /**
     * 记录查询日志
     */
    private void logQuery(String requestId, String clientIp, AssistantRequest request,
                          List<RetrievalResult> results, ChatCompletionResponse llmResponse,
                          AssistantResponse response, boolean success) {
        try {
            AssistantLog log = new AssistantLog();
            log.setRequestId(requestId);
            log.setClientIp(clientIp);
            log.setQuestion(request.getQuestion());
            log.setMode(request.getMode());
            log.setSuccess(success);
            
            if (results != null && !results.isEmpty()) {
                String articleIds = results.stream()
                    .map(r -> String.valueOf(r.getArticleId()))
                    .distinct()
                    .collect(Collectors.joining(","));
                log.setHitArticleIds(articleIds);
            }
            
            if (response != null) {
                log.setCitationsCount(response.getCitations().size());
                log.setLatencyMs(response.getLatencyMs());
            }
            
            if (llmResponse != null) {
                log.setLlmModel(llmResponse.getModel());
                if (llmResponse.getUsage() != null) {
                    log.setTokenUsage(objectMapper.writeValueAsString(llmResponse.getUsage()));
                }
            }
            
            assistantLogRepository.save(log);
        } catch (Exception e) {
            // 日志记录失败不影响主流程
            this.log.error("记录查询日志失败", e);
        }
    }
    
    /**
     * 截断文本
     */
    private String truncate(String text, int maxLength) {
        if (text == null) return "";
        if (text.length() <= maxLength) return text;
        return text.substring(0, maxLength) + "...";
    }
    
    /**
     * float[] 转 List<Float>
     */
    private List<Float> floatArrayToList(float[] array) {
        List<Float> list = new ArrayList<>(array.length);
        for (float f : array) {
            list.add(f);
        }
        return list;
    }
    
    /**
     * 流式查询（SSE）
     */
    public void queryStream(AssistantRequest request, String clientIp, SseEmitter emitter) {
        String requestId = UUID.randomUUID().toString();
        long startTime = System.currentTimeMillis();
        
        try {
            log.info("收到流式查询请求: request_id={}, question={}, mode={}", 
                requestId, request.getQuestion(), request.getMode());
            
            // 1. 生成 embedding
            float[] queryEmbedding = llmService.generateEmbedding(request.getQuestion());
            log.debug("Embedding 生成完成: request_id={}, dim={}", requestId, queryEmbedding.length);
            
            // 2. 混合检索
            List<RetrievalResult> results = hybridSearch(request.getQuestion(), queryEmbedding);
            log.info("检索完成: request_id={}, 检索到 {} 条结果", requestId, 
                results != null ? results.size() : 0);
            
            // 3. 过滤高相关度文章（>= 80%）
            List<RetrievalResult> highRelevanceResults = filterHighRelevanceResults(results);
            log.info("相关度过滤: request_id={}, 检索到 {} 条结果, 高相关度（>=80%）{} 条", 
                requestId, results != null ? results.size() : 0, highRelevanceResults.size());
            
            // 4. 判断模式
            boolean hasArticles = !highRelevanceResults.isEmpty();
            boolean isFlexibleMode = "FLEXIBLE".equalsIgnoreCase(request.getMode());
            log.info("查询模式: request_id={}, hasHighRelevanceArticles={}, isFlexibleMode={}", 
                requestId, hasArticles, isFlexibleMode);
            
            // 5. 构建消息列表（包含历史对话）
            List<ChatCompletionRequest.ChatMessage> messages = new ArrayList<>();
            
            String systemPrompt = (hasArticles || !isFlexibleMode) ? 
                SYSTEM_PROMPT_WITH_ARTICLES : SYSTEM_PROMPT_FLEXIBLE;
            messages.add(new ChatCompletionRequest.ChatMessage("system", systemPrompt));
            
            if (request.getHistory() != null && !request.getHistory().isEmpty()) {
                for (AssistantRequest.ChatMessage historyMsg : request.getHistory()) {
                    messages.add(new ChatCompletionRequest.ChatMessage(
                        historyMsg.getRole(), 
                        historyMsg.getContent()
                    ));
                }
            }
            
            String userPrompt = hasArticles ? 
                buildPrompt(request.getQuestion(), highRelevanceResults) : request.getQuestion();
            messages.add(new ChatCompletionRequest.ChatMessage("user", userPrompt));
            
            // 6. 流式调用 LLM
            if (hasArticles || isFlexibleMode) {
                log.info("开始流式生成: request_id={}, 基于 {} 篇高相关度文章", 
                    requestId, hasArticles ? highRelevanceResults.size() : 0);
                final int[] chunkCount = {0};
                
                llmService.chatCompletionStream(messages, 2048, (chunk) -> {
                    try {
                        chunkCount[0]++;
                        emitter.send(SseEmitter.event()
                            .name("message")
                            .data(chunk));
                    } catch (IOException e) {
                        log.error("发送 SSE chunk 失败: request_id={}, chunk_index={}", 
                            requestId, chunkCount[0], e);
                    }
                });
                
                log.info("流式生成完成: request_id={}, 共发送 {} 个 chunks", requestId, chunkCount[0]);
            } else {
                log.info("ARTICLE_ONLY 模式且无高相关度文章，返回提示信息: request_id={}", requestId);
                emitter.send(SseEmitter.event()
                    .name("message")
                    .data("抱歉，文章库中未找到与您问题高度相关的内容（相关度 < 80%）。"));
            }
            
            // 7. 发送引用（只有高相关度文章才发送）
            if (hasArticles) {
                List<AssistantResponse.Citation> citations = extractCitations(highRelevanceResults);
                log.info("发送引用: request_id={}, 去重后文章数={}", requestId, citations.size());
                if (!citations.isEmpty()) {
                    emitter.send(SseEmitter.event()
                        .name("citations")
                        .data(new ObjectMapper().writeValueAsString(citations)));
                }
            } else {
                // 无高相关度文章，也不发送引用
                log.info("无高相关度文章（< 80%），不发送引用: request_id={}", requestId);
            }
            
            // 7. 完成
            long latency = System.currentTimeMillis() - startTime;
            log.info("查询完成: request_id={}, latency={}ms", requestId, latency);
            emitter.send(SseEmitter.event()
                .name("done")
                .data("{\"latencyMs\":" + latency + "}"));
            emitter.complete();
            
        } catch (Exception e) {
            log.error("RAG 流式查询失败: request_id={}, question={}, error={}", 
                requestId, request.getQuestion(), e.getMessage(), e);
            try {
                String errorMsg = e.getMessage() != null ? e.getMessage() : "未知错误";
                emitter.send(SseEmitter.event()
                    .name("error")
                    .data("{\"message\":\"" + errorMsg.replace("\"", "\\\"") + "\"}"));
                emitter.complete();
            } catch (IOException ex) {
                log.error("发送错误信息失败: request_id={}", requestId, ex);
            }
        }
    }
    
    /**
     * 检索结果（内部使用）
     */
    @Data
    private static class RetrievalResult {
        private String chunkId;
        private Long articleId;
        private String slug;
        private String title;
        private String anchor;
        private String chunkText;
        private Double vectorScore = 0.0;
        private Double bm25Score = 0.0;
        private Double finalScore = 0.0;
    }
}
