package com.lingdang.blog.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lingdang.blog.config.ElasticsearchInitializer;
import com.lingdang.blog.dto.assistant.AssistantRequest;
import com.lingdang.blog.dto.assistant.AssistantResponse;
import com.lingdang.blog.dto.assistant.RagConfigDTO;
import com.lingdang.blog.dto.llm.ChatCompletionRequest;
import com.lingdang.blog.dto.llm.ChatCompletionResponse;
import com.lingdang.blog.model.AssistantLog;
import com.lingdang.blog.model.ChunkDocument;
import com.lingdang.blog.model.RagQueryHit;
import com.lingdang.blog.model.RagQueryLog;
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

    @Autowired
    private RagObservabilityService ragObservabilityService;

    @Autowired
    private RagConfigService ragConfigService;
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    // System Prompt
    private static final String SYSTEM_PROMPT_WITH_ARTICLES = """
        你是铃铛师兄大模型博客的智能助手。你的任务是基于提供的文章片段回答用户的问题。
        
        **回答规则**：
        1. 只基于提供的参考文章回答，不要编造信息
        2. 如果文章中没有相关信息，明确告知用户
        3. 回答要准确、简洁、专业
        4. **引用格式**：在回答中引用文章时，使用角标 [1]、[2] 等标注来源，数字对应参考文章列表的编号
        5. 保持中文回答
        
        **Markdown 格式要求**（非常重要）：
        - 标题使用 ## 或 ###，且**前后必须有空行**
        - 列表使用 - 或 1. 2. 3.，列表前需要空行
        - 代码使用 ``` 包裹
        - 重点内容使用 **粗体**
        """;

    private static final String SYSTEM_PROMPT_WITH_ARTICLES_NO_CITATION = """
        你是铃铛师兄大模型博客的智能助手。你的任务是基于提供的文章片段回答用户的问题。
        
        **回答规则**：
        1. 只基于提供的参考文章回答，不要编造信息
        2. 如果文章中没有相关信息，明确告知用户
        3. 回答要准确、简洁、专业
        4. 保持中文回答
        
        **Markdown 格式要求**（非常重要）：
        - 标题使用 ## 或 ###，且**前后必须有空行**
        - 列表使用 - 或 1. 2. 3.，列表前需要空行
        - 代码使用 ``` 包裹
        - 重点内容使用 **粗体**
        """;
    
    private static final String SYSTEM_PROMPT_FLEXIBLE = """
        你是铃铛师兄大模型博客的智能助手，也是大模型和AI领域的专家。
        
        **你的职责**：
        1. 如果提供了相关文章片段，优先基于文章内容回答，并标注引用来源 [1]、[2]
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
        
        **Markdown 格式要求**（非常重要）：
        - 标题使用 ## 或 ###，且**前后必须有空行**
        - 列表使用 - 或 1. 2. 3.，列表前需要空行
        - 代码使用 ``` 包裹
        - 重点内容使用 **粗体**
        """;

    private static final String SYSTEM_PROMPT_FLEXIBLE_NO_CITATION = """
        你是铃铛师兄大模型博客的智能助手，也是大模型和AI领域的专家。
        
        **你的职责**：
        1. 如果提供了相关文章片段，优先基于文章内容回答
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
        
        **Markdown 格式要求**（非常重要）：
        - 标题使用 ## 或 ###，且**前后必须有空行**
        - 列表使用 - 或 1. 2. 3.，列表前需要空行
        - 代码使用 ``` 包裹
        - 重点内容使用 **粗体**
        """;

    private String selectSystemPrompt(boolean hasArticles, boolean isFlexibleMode, boolean returnCitations) {
        if (hasArticles || !isFlexibleMode) {
            return returnCitations ? SYSTEM_PROMPT_WITH_ARTICLES : SYSTEM_PROMPT_WITH_ARTICLES_NO_CITATION;
        }
        return returnCitations ? SYSTEM_PROMPT_FLEXIBLE : SYSTEM_PROMPT_FLEXIBLE_NO_CITATION;
    }
    
    /**
     * 查询 Assistant
     */
    public AssistantResponse query(AssistantRequest request, String clientIp) {
        long startTime = System.currentTimeMillis();
        String requestId = UUID.randomUUID().toString();
        
        try {
            RagConfigDTO ragConfig = ragConfigService.getConfig();
            int topK = ragConfig.getTopK() != null ? ragConfig.getTopK() : 5;
            double minScore = ragConfig.getMinScore() != null ? ragConfig.getMinScore() : 0.0;
            boolean returnCitations = Boolean.TRUE.equals(ragConfig.getReturnCitations());

            // 1. 生成查询向量
            float[] queryEmbedding = llmService.generateEmbedding(request.getQuestion());
            
            // 2. 混合检索（向量 + BM25）
            HybridSearchResult hybrid = hybridSearch(request.getQuestion(), queryEmbedding, topK);
            List<RetrievalResult> results = hybrid.merged;
            
            // 3. 过滤高相关度文章
            List<RetrievalResult> highRelevanceResults = filterHighRelevanceResults(results, minScore, topK);
            log.info("相关度过滤: 检索到 {} 条结果, 阈值>={}, 过滤后 {} 条", 
                results != null ? results.size() : 0, minScore, highRelevanceResults.size());
            
            // 4. 判断模式和是否有高相关度检索结果
            boolean hasArticles = !highRelevanceResults.isEmpty();
            boolean isFlexibleMode = "FLEXIBLE".equalsIgnoreCase(request.getMode());
            
            // 5. 构建消息列表（包含历史对话）
            List<ChatCompletionRequest.ChatMessage> messages = new ArrayList<>();
            
            // 添加系统提示
            String systemPrompt = selectSystemPrompt(hasArticles, isFlexibleMode, returnCitations);
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
                buildPrompt(request.getQuestion(), highRelevanceResults, returnCitations) : request.getQuestion();
            messages.add(new ChatCompletionRequest.ChatMessage("user", userPrompt));
            
            // 6. 调用 LLM
            String answer;
            List<AssistantResponse.Citation> citations;
            ChatCompletionResponse llmResponse;
            
            if (hasArticles || isFlexibleMode) {
                llmResponse = llmService.chatCompletionWithUsage(messages, 2048);
                answer = llmResponse.getChoices().get(0).getMessage().getContent();
                citations = (hasArticles && returnCitations) ? extractCitations(highRelevanceResults) : new ArrayList<>();
            } else {
                // ARTICLE_ONLY 模式且无高相关度文章：返回未找到
                answer = "抱歉，文章库中未找到满足相关度阈值的内容。您可以尝试换个问法，或者查看文章列表选择感兴趣的文章阅读。";
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

            // 8.1 记录 RAG 评估日志（仅用于 Studio）
            try {
                RagConfigDTO cfg = ragConfigService.getConfig();
                RagQueryLog ragLog = ragObservabilityService.buildBaseLog(requestId, clientIp, request.getQuestion(), cfg);
                ragLog.setHasArticles(!highRelevanceResults.isEmpty());
                ragLog.setVectorCandidates(hybrid.vectorCount);
                ragLog.setBm25Candidates(hybrid.bm25Count);
                ragLog.setFilteredCandidates(highRelevanceResults.size());
                ragLog.setCitationsCount(response.getCitations() != null ? response.getCitations().size() : 0);
                ragLog.setLatencyMs(response.getLatencyMs());
                ragLog.setSuccess(true);
                String hitIds = highRelevanceResults.stream()
                    .map(RetrievalResult::getArticleId)
                    .filter(java.util.Objects::nonNull)
                    .distinct()
                    .map(String::valueOf)
                    .collect(java.util.stream.Collectors.joining(","));
                ragLog.setHitArticleIds(hitIds);
                ragObservabilityService.upsertQueryLog(ragLog);
            } catch (Exception ignored) {
            }

            
            return response;
            
        } catch (Exception e) {
            log.error("RAG 查询失败: request_id={}", requestId, e);
            
            // 记录失败日志
            logQuery(requestId, clientIp, request, Collections.emptyList(), null, null, false);
            
            throw new RuntimeException("查询失败: " + e.getMessage(), e);
        }
    }
    
    private static class HybridSearchResult {
        private final List<RetrievalResult> merged;
        private final int vectorCount;
        private final int bm25Count;

        private HybridSearchResult(List<RetrievalResult> merged, int vectorCount, int bm25Count) {
            this.merged = merged;
            this.vectorCount = vectorCount;
            this.bm25Count = bm25Count;
        }
    }

    /**
     * 混合检索（向量 + BM25）
     */
    private HybridSearchResult hybridSearch(String question, float[] queryEmbedding, int topK) throws IOException {
        int safeTopK = Math.max(topK, 1);
        int vectorTopK = Math.min(Math.max(safeTopK * 10, 50), 100);
        int bm25TopK = Math.min(Math.max(safeTopK * 4, 20), 100);

        // 向量检索
        List<RetrievalResult> vectorResults = vectorSearch(queryEmbedding, vectorTopK);

        // BM25 检索
        List<RetrievalResult> bm25Results = bm25Search(question, bm25TopK);

        // 合并去重并重排序
        List<RetrievalResult> merged = mergeAndRerank(vectorResults, bm25Results, safeTopK);
        return new HybridSearchResult(merged,
            vectorResults != null ? vectorResults.size() : 0,
            bm25Results != null ? bm25Results.size() : 0);

    }
    
    /**
     * 向量检索
     */
    private List<RetrievalResult> vectorSearch(float[] embedding, int topK) throws IOException {
        List<RetrievalResult> results = new ArrayList<>();
        
        try {
            // 先检查索引是否存在
            boolean indexExists = esClient.indices().exists(e -> e.index(ElasticsearchInitializer.INDEX_ALIAS)).value();

            if (!indexExists) {
                log.warn("索引 {} 不存在，跳过向量检索", ElasticsearchInitializer.INDEX_ALIAS);
                return results;
            }

            // 检查索引是否有数据
            long count = esClient.count(c -> c.index(ElasticsearchInitializer.INDEX_ALIAS)).count();
            if (count == 0) {
                log.warn("索引 {} 为空，跳过向量检索", ElasticsearchInitializer.INDEX_ALIAS);
                return results;
            }

            log.info("开始向量检索: index={}, topK={}, embedding_dim={}, doc_count={}",
                ElasticsearchInitializer.INDEX_ALIAS, topK, embedding.length, count);

            SearchResponse<ChunkDocument> response = esClient.search(s -> s
                .index(ElasticsearchInitializer.INDEX_ALIAS)
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
            boolean indexExists = esClient.indices().exists(e -> e.index(ElasticsearchInitializer.INDEX_ALIAS)).value();

            if (!indexExists) {
                log.warn("索引 {} 不存在，跳过 BM25 检索", ElasticsearchInitializer.INDEX_ALIAS);
                return results;
            }

            // 检查索引是否有数据
            long count = esClient.count(c -> c.index(ElasticsearchInitializer.INDEX_ALIAS)).count();
            if (count == 0) {
                log.warn("索引 {} 为空，跳过 BM25 检索", ElasticsearchInitializer.INDEX_ALIAS);
                return results;
            }

            log.info("开始 BM25 检索: index={}, query='{}', topK={}, doc_count={}",
                ElasticsearchInitializer.INDEX_ALIAS, query, topK, count);

            SearchResponse<ChunkDocument> response = esClient.search(s -> s
                .index(ElasticsearchInitializer.INDEX_ALIAS)
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
     * 
     * 重要：按文章（articleId）去重并编号，确保 LLM 生成的引用 [1]、[2] 与参考文章列表对应
     */
    private String buildPrompt(String question, List<RetrievalResult> results, boolean includeCitations) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("**用户问题**：\n").append(question).append("\n\n");
        prompt.append("**参考文章**：\n\n");
        
        // 按 articleId 去重，保留每篇文章所有相关 chunks（但编号按文章）
        Map<Long, List<RetrievalResult>> articleChunks = new LinkedHashMap<>();
        for (RetrievalResult result : results) {
            articleChunks.computeIfAbsent(result.getArticleId(), k -> new ArrayList<>()).add(result);
        }
        
        int articleIndex = 1;
        for (Map.Entry<Long, List<RetrievalResult>> entry : articleChunks.entrySet()) {
            List<RetrievalResult> chunks = entry.getValue();
            String title = chunks.get(0).getTitle();
            
            if (includeCitations) {
                prompt.append(String.format("[%d] 《%s》\n", articleIndex, title));
            } else {
                prompt.append(String.format("《%s》\n", title));
            }
            
            // 合并同一篇文章的所有 chunks
            for (RetrievalResult chunk : chunks) {
                prompt.append(chunk.getChunkText()).append("\n\n");
            }
            
            articleIndex++;
        }
        
        if (includeCitations) {
            prompt.append("**请基于以上参考文章回答用户问题，在回答中用 [1]、[2] 等角标标注引用来源**。");
        } else {
            prompt.append("**请基于以上参考文章回答用户问题**。");
        }
        
        return prompt.toString();
    }
    
    /**
     * 过滤高相关度文章
     */
    private List<RetrievalResult> filterHighRelevanceResults(List<RetrievalResult> results, double minScore, int maxResults) {
        if (results == null || results.isEmpty()) {
            return new ArrayList<>();
        }

        double threshold = Math.max(minScore, 0.0);
        int limit = Math.max(maxResults, 1);

        return results.stream()
            .filter(r -> r.getFinalScore() >= threshold)
            .limit(limit)
            .collect(Collectors.toList());
    }
    
    /**
     * 提取引用（只提取已经过滤的高相关度文章）
     * 
     * 重要：按 articleId 去重，同一篇文章只显示一次
     * 编号顺序与 buildPrompt 中的文章编号保持一致（按出现顺序）
     */
    private List<AssistantResponse.Citation> extractCitations(List<RetrievalResult> results) {
        // 按 articleId 去重，保持原始顺序（使用 LinkedHashMap）
        Map<Long, RetrievalResult> bestChunkPerArticle = new LinkedHashMap<>();
        
        for (RetrievalResult result : results) {
            Long articleId = result.getArticleId();
            if (!bestChunkPerArticle.containsKey(articleId) || 
                result.getFinalScore() > bestChunkPerArticle.get(articleId).getFinalScore()) {
                bestChunkPerArticle.put(articleId, result);
            }
        }
        
        // 转换为 Citation 列表，添加 refIndex（从 1 开始编号）
        List<AssistantResponse.Citation> citations = new ArrayList<>();
        int refIndex = 1;
        
        for (RetrievalResult result : bestChunkPerArticle.values()) {
            AssistantResponse.Citation citation = new AssistantResponse.Citation();
            citation.setRefIndex(refIndex++); // 引用编号，对应答案中的 [1]、[2]
            citation.setTitle(result.getTitle());
            citation.setUrl("/blog/" + result.getSlug() + 
                (result.getAnchor() != null && !result.getAnchor().isEmpty() ? "#" + result.getAnchor() : ""));
            citation.setQuote(""); // 不显示文章内容，只显示标题
            citation.setChunkId(result.getChunkId());
            citation.setScore(result.getFinalScore()); // 已归一化到 0.0 ~ 1.0
            citations.add(citation);
        }
        
        return citations;
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

        RagQueryLog ragLog = null;
        long retrievalStart = System.currentTimeMillis();

        try {
            RagConfigDTO ragConfig = ragConfigService.getConfig();
            int topK = ragConfig.getTopK() != null ? ragConfig.getTopK() : 5;
            double minScore = ragConfig.getMinScore() != null ? ragConfig.getMinScore() : 0.0;
            boolean returnCitations = Boolean.TRUE.equals(ragConfig.getReturnCitations());

            // 记录基础日志（后续补齐字段）
            ragLog = ragObservabilityService.buildBaseLog(requestId, clientIp, request.getQuestion(), ragConfig);
            ragLog.setSuccess(true);
            ragObservabilityService.upsertQueryLog(ragLog);

            log.info("收到流式查询请求: request_id={}, question={}, mode={}", 
                requestId, request.getQuestion(), request.getMode());
            
            // 1. 生成 embedding
            float[] queryEmbedding = llmService.generateEmbedding(request.getQuestion());
            log.debug("Embedding 生成完成: request_id={}, dim={}", requestId, queryEmbedding.length);
            
            // 2. 混合检索
            HybridSearchResult hybrid = hybridSearch(request.getQuestion(), queryEmbedding, topK);
            List<RetrievalResult> results = hybrid.merged;
            log.info("检索完成: request_id={}, merged={}, vector={}, bm25={}", requestId,
                results != null ? results.size() : 0, hybrid.vectorCount, hybrid.bm25Count);

            // 3. 过滤高相关度文章
            List<RetrievalResult> highRelevanceResults = filterHighRelevanceResults(results, minScore, topK);
            log.info("相关度过滤: request_id={}, merged={}, 阈值>={}, 过滤后 {} 条",
                requestId, results != null ? results.size() : 0, minScore, highRelevanceResults.size());


            // 记录检索阶段指标 + top hits（用于 7 天留存的调参数据）
            if (ragLog != null) {
                ragLog.setHasArticles(!highRelevanceResults.isEmpty());
                ragLog.setFilteredCandidates(highRelevanceResults.size());
                ragLog.setRetrievalMs((int) (System.currentTimeMillis() - retrievalStart));
                ragLog.setVectorCandidates(hybrid.vectorCount);
                ragLog.setBm25Candidates(hybrid.bm25Count);
                // 记录命中的 articleId 列表
                String hitIds = highRelevanceResults.stream()
                    .map(RetrievalResult::getArticleId)
                    .filter(java.util.Objects::nonNull)
                    .distinct()
                    .map(String::valueOf)
                    .collect(java.util.stream.Collectors.joining(","));
                ragLog.setHitArticleIds(hitIds);
                ragObservabilityService.upsertQueryLog(ragLog);

                int hitLimit = Math.min(highRelevanceResults.size(), 20);
                List<RagQueryHit> hits = new ArrayList<>();
                for (int i = 0; i < hitLimit; i++) {
                    RetrievalResult r = highRelevanceResults.get(i);
                    RagQueryHit h = new RagQueryHit();
                    h.setRequestId(requestId);
                    h.setRankNo(i + 1);
                    h.setChunkId(r.getChunkId());
                    h.setArticleId(r.getArticleId());
                    h.setSlug(r.getSlug());
                    h.setTitle(r.getTitle());
                    h.setVectorScore(r.getVectorScore());
                    h.setBm25Score(r.getBm25Score());
                    h.setFinalScore(r.getFinalScore());
                    hits.add(h);
                }
                ragObservabilityService.replaceHits(requestId, hits);
            }
            
            // 4. 判断模式
            boolean hasArticles = !highRelevanceResults.isEmpty();
            boolean isFlexibleMode = "FLEXIBLE".equalsIgnoreCase(request.getMode());
            log.info("查询模式: request_id={}, hasHighRelevanceArticles={}, isFlexibleMode={}", 
                requestId, hasArticles, isFlexibleMode);
            
            // 5. 构建消息列表（包含历史对话）
            List<ChatCompletionRequest.ChatMessage> messages = new ArrayList<>();
            
            String systemPrompt = selectSystemPrompt(hasArticles, isFlexibleMode, returnCitations);
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
                buildPrompt(request.getQuestion(), highRelevanceResults, returnCitations) : request.getQuestion();
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
                    .data("抱歉，文章库中未找到满足相关度阈值的内容。"));
            }
            
            // 7. 发送引用（只有高相关度文章才发送）
            int citationsCount = 0;
            if (hasArticles && returnCitations) {
                List<AssistantResponse.Citation> citations = extractCitations(highRelevanceResults);
                citationsCount = citations.size();
                log.info("发送引用: request_id={}, 去重后文章数={}", requestId, citations.size());
                if (!citations.isEmpty()) {
                    emitter.send(SseEmitter.event()
                        .name("citations")
                        .data(new ObjectMapper().writeValueAsString(citations)));
                }
            } else {
                // 无高相关度文章，也不发送引用
                log.info("无高相关度文章或关闭引用返回，不发送引用: request_id={}", requestId);
            }

            // 8. 完成
            long latency = System.currentTimeMillis() - startTime;
            log.info("查询完成: request_id={}, latency={}ms", requestId, latency);

            if (ragLog != null) {
                ragLog.setCitationsCount(citationsCount);
                ragLog.setLatencyMs((int) latency);
                ragLog.setSuccess(true);
                ragObservabilityService.upsertQueryLog(ragLog);
            }

            emitter.send(SseEmitter.event()
                .name("done")
                .data("{\"latencyMs\":" + latency + "}"));
            emitter.complete();
            
        } catch (Exception e) {
            log.error("RAG 流式查询失败: request_id={}, question={}, error={}",
                requestId, request.getQuestion(), e.getMessage(), e);

            if (ragLog != null) {
                ragLog.setSuccess(false);
                ragLog.setErrorMessage(e.getMessage());
                ragLog.setLatencyMs((int) (System.currentTimeMillis() - startTime));
                ragObservabilityService.upsertQueryLog(ragLog);
            }

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
