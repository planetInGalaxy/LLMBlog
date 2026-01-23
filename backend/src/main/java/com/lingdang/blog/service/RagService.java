package com.lingdang.blog.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
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

import java.io.IOException;
import java.time.LocalDateTime;
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
            
            // 3. 判断模式和是否有检索结果
            boolean hasArticles = results != null && !results.isEmpty();
            boolean isFlexibleMode = "FLEXIBLE".equalsIgnoreCase(request.getMode());
            
            String answer;
            List<AssistantResponse.Citation> citations;
            ChatCompletionResponse llmResponse;
            
            if (hasArticles) {
                // 有文章：基于文章回答
                String userPrompt = buildPrompt(request.getQuestion(), results);
                String systemPrompt = isFlexibleMode ? SYSTEM_PROMPT_FLEXIBLE : SYSTEM_PROMPT_WITH_ARTICLES;
                
                List<ChatCompletionRequest.ChatMessage> messages = Arrays.asList(
                    new ChatCompletionRequest.ChatMessage("system", systemPrompt),
                    new ChatCompletionRequest.ChatMessage("user", userPrompt)
                );
                
                llmResponse = llmService.chatCompletionWithUsage(messages, 2048);
                answer = llmResponse.getChoices().get(0).getMessage().getContent();
                citations = extractCitations(results);
                
            } else if (isFlexibleMode) {
                // FLEXIBLE 模式且无文章：直接回答
                List<ChatCompletionRequest.ChatMessage> messages = Arrays.asList(
                    new ChatCompletionRequest.ChatMessage("system", SYSTEM_PROMPT_FLEXIBLE),
                    new ChatCompletionRequest.ChatMessage("user", request.getQuestion())
                );
                
                llmResponse = llmService.chatCompletionWithUsage(messages, 2048);
                answer = llmResponse.getChoices().get(0).getMessage().getContent();
                citations = new ArrayList<>();
                
            } else {
                // ARTICLE_ONLY 模式且无文章：返回未找到
                answer = "抱歉，文章库中未找到与您问题相关的内容。您可以尝试换个问法，或者查看文章列表选择感兴趣的文章阅读。";
                citations = new ArrayList<>();
                llmResponse = null;
            }
            
            // 6. 构建响应
            AssistantResponse response = new AssistantResponse();
            response.setAnswer(answer);
            response.setCitations(citations);
            response.setQueryId(requestId);
            response.setLatencyMs((int) (System.currentTimeMillis() - startTime));
            
            // 7. 记录日志
            logQuery(requestId, clientIp, request, results, llmResponse, response, true);
            
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
                    result.setVectorScore(hit.score() != null ? hit.score() : 0.0);
                    results.add(result);
                }
            }
        } catch (Exception e) {
            log.error("向量检索失败", e);
        }
        
        return results;
    }
    
    /**
     * BM25 检索
     */
    private List<RetrievalResult> bm25Search(String query, int topK) throws IOException {
        List<RetrievalResult> results = new ArrayList<>();
        
        try {
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
                    result.setBm25Score(hit.score() != null ? hit.score() : 0.0);
                    results.add(result);
                }
            }
        } catch (Exception e) {
            log.error("BM25 检索失败", e);
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
        
        // 重排序：0.7 * vector + 0.3 * bm25
        return merged.values().stream()
            .peek(r -> r.setFinalScore(0.7 * r.getVectorScore() + 0.3 * r.getBm25Score()))
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
     * 提取引用
     */
    private List<AssistantResponse.Citation> extractCitations(List<RetrievalResult> results) {
        return results.stream()
            .limit(6)
            .map(result -> {
                AssistantResponse.Citation citation = new AssistantResponse.Citation();
                citation.setTitle(result.getTitle());
                citation.setUrl("/blog/" + result.getSlug() + 
                    (result.getAnchor() != null && !result.getAnchor().isEmpty() ? "#" + result.getAnchor() : ""));
                citation.setQuote(truncate(result.getChunkText(), 200));
                citation.setChunkId(result.getChunkId());
                citation.setScore(result.getFinalScore());
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
