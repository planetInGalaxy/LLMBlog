package com.lingdang.blog.config;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.indices.CreateIndexRequest;
import co.elastic.clients.elasticsearch.indices.ExistsRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Elasticsearch 索引初始化器
 * 应用启动时自动创建索引（如果不存在）
 */
@Slf4j
@Component
public class ElasticsearchInitializer {
    
    @Autowired
    private ElasticsearchClient esClient;
    
    /**
     * 读写别名（线上永远通过 alias 访问，便于蓝绿重建索引）
     */
    public static final String INDEX_ALIAS = "lingdang_chunks";

    /**
     * 旧版本固定索引名（历史兼容）
     */
    public static final String LEGACY_INDEX = "lingdang_chunks_v1";

    private static final String INDEX_PREFIX = "lingdang_chunks_v1_";
    
    /**
     * 应用就绪后初始化索引
     */
    @EventListener(ApplicationReadyEvent.class)
    public void initializeIndex() {
        try {
            log.info("=== Elasticsearch 索引初始化开始 ===");
            log.info("目标别名: {}", INDEX_ALIAS);

            // 检查 ES 连接
            try {
                boolean pingResult = esClient.ping().value();
                if (!pingResult) {
                    log.error("❌ Elasticsearch 连接失败");
                    return;
                }
                log.info("✅ Elasticsearch 连接正常");
            } catch (Exception e) {
                log.error("❌ Elasticsearch 连接异常: {}", e.getMessage());
                return;
            }
            
            // 1) 确保 alias 存在（兼容旧索引：legacy -> alias）
            ensureAlias();

            // 2) 如果 alias 指向的索引 embedding 维度不匹配，则重建一个新索引并切换 alias
            try {
                String currentIndex = resolveCurrentIndex();
                if (currentIndex != null) {
                    Integer dims = readEmbeddingDims(currentIndex);
                    int expectedDims = 768; // 与 ChunkDocument 中定义一致
                    if (dims != null && dims != expectedDims) {
                        log.warn("⚠️  检测到 embedding 维度不匹配: currentDims={}, expectedDims={}，将自动重建索引并切换 alias", dims, expectedDims);
                        String newIndex = createNewConcreteIndex();
                        switchAliasTo(newIndex);
                    }
                }
            } catch (Exception e) {
                log.warn("检查/修复 embedding 维度失败（将继续使用现有索引）: {}", e.getMessage());
            }

            // 3) 打印当前文档数
            try {
                long count = esClient.count(c -> c.index(INDEX_ALIAS)).count();
                log.info("📊 当前索引(alias={})文档数量: {}", INDEX_ALIAS, count);
            } catch (Exception e) {
                log.warn("获取索引文档数量失败: {}", e.getMessage());
            }

            log.info("=== Elasticsearch 索引初始化完成 ===");
            
        } catch (Exception e) {
            log.error("❌ 索引初始化失败: {}", e.getMessage(), e);
            log.warn("⚠️  索引初始化失败不影响应用启动");
            log.warn("⚠️  可通过 Studio 的「全量重建索引」功能手动修复");
        }
    }
    
    /**
     * 检查索引健康状态（提供给 Controller 调用）
     */
    public IndexHealth checkIndexHealth() {
        IndexHealth health = new IndexHealth();
        health.setIndexName(INDEX_ALIAS);

        try {
            // 检查 ES 连接
            boolean pingResult = esClient.ping().value();
            health.setEsConnected(pingResult);

            if (!pingResult) {
                health.setHealthy(false);
                health.setMessage("Elasticsearch 连接失败");
                return health;
            }

            // alias 是否存在（以及是否有指向的实际索引）
            String currentIndex = resolveCurrentIndex();
            boolean exists = currentIndex != null;
            health.setIndexExists(exists);

            if (!exists) {
                health.setHealthy(false);
                health.setMessage("索引别名不存在或未绑定索引，请执行全量重建索引");
                return health;
            }

            // 获取 chunks 总数
            long chunkCount = esClient.count(c -> c.index(INDEX_ALIAS)).count();
            health.setDocumentCount(chunkCount);

            // 获取去重后的文章数量（使用 cardinality aggregation）
            var aggResponse = esClient.search(s -> s
                .index(INDEX_ALIAS)
                .size(0)
                .aggregations("unique_articles", a -> a
                    .cardinality(c -> c.field("articleId"))
                ), Object.class);

            long articleCount = aggResponse.aggregations()
                .get("unique_articles")
                .cardinality()
                .value();

            health.setArticleCount(articleCount);
            health.setHealthy(true);
            health.setMessage("索引健康");

        } catch (Exception e) {
            health.setHealthy(false);
            health.setMessage("检查失败: " + e.getMessage());
        }

        return health;
    }
    
    private void ensureAlias() throws Exception {
        // 如果 alias 已经存在（有指向），直接返回
        String current = resolveCurrentIndex();
        if (current != null) {
            log.info("✅ 索引 alias 已存在: {} -> {}", INDEX_ALIAS, current);
            return;
        }

        // 兼容：如果 legacy 索引存在，则创建 alias 指向它
        boolean legacyExists = esClient.indices().exists(e -> e.index(LEGACY_INDEX)).value();
        if (legacyExists) {
            log.info("检测到 legacy 索引存在，将创建 alias: {} -> {}", INDEX_ALIAS, LEGACY_INDEX);
            esClient.indices().putAlias(a -> a.index(LEGACY_INDEX).name(INDEX_ALIAS));
            return;
        }

        // 否则创建一个全新索引并绑定 alias
        String newIndex = createNewConcreteIndex();
        switchAliasTo(newIndex);
    }

    /**
     * 返回 alias 当前指向的实际索引名；如果不存在返回 null。
     */
    public String resolveCurrentIndex() {
        try {
            var resp = esClient.indices().getAlias(a -> a.name(INDEX_ALIAS));
            if (resp == null || resp.result() == null || resp.result().isEmpty()) {
                return null;
            }
            // result 的 key 是 indexName
            return resp.result().keySet().iterator().next();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 创建一个新的实际索引（不绑定 alias），用于蓝绿重建。
     */
    public String createNewConcreteIndex() throws Exception {
        String indexName = INDEX_PREFIX + System.currentTimeMillis();

        // settings
        final String settingsJson;
        try (InputStream is = new ClassPathResource("elasticsearch/chunk-settings.json").getInputStream()) {
            settingsJson = new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }

        CreateIndexRequest req = CreateIndexRequest.of(c -> c
            .index(indexName)
            .settings(s -> s.withJson(new java.io.StringReader(settingsJson)))
            .mappings(m -> m
                .properties("chunkId", p -> p.keyword(k -> k))
                .properties("articleId", p -> p.long_(l -> l))
                .properties("slug", p -> p.keyword(k -> k))
                .properties("title", p -> p.text(t -> t.analyzer("ik_max_word")))
                .properties("tags", p -> p.text(t -> t.analyzer("ik_max_word")))
                .properties("status", p -> p.keyword(k -> k))
                .properties("indexVersion", p -> p.integer(i -> i))
                .properties("headingLevel", p -> p.integer(i -> i))
                .properties("headingText", p -> p.text(t -> t.analyzer("ik_max_word")))
                .properties("anchor", p -> p.keyword(k -> k))
                .properties("chunkText", p -> p.text(t -> t.analyzer("ik_max_word")))
                .properties("embedding", p -> p.denseVector(v -> v.dims(768)))
                .properties("tokenCount", p -> p.integer(i -> i))
                .properties("sequenceNumber", p -> p.integer(i -> i))
            )
        );

        esClient.indices().create(req);
        log.info("✅ 已创建新索引: {}", indexName);
        return indexName;
    }

    /**
     * 原子切换 alias 指向指定索引。
     */
    public void switchAliasTo(String newIndex) throws Exception {
        final String oldIndex = resolveCurrentIndex();

        esClient.indices().updateAliases(a -> {
            if (oldIndex != null) {
                a.actions(act -> act.remove(r -> r.index(oldIndex).alias(INDEX_ALIAS)));
            }
            a.actions(act -> act.add(ad -> ad.index(newIndex).alias(INDEX_ALIAS)));
            return a;
        });

        log.info("✅ alias 已切换: {} -> {} (old={})", INDEX_ALIAS, newIndex, oldIndex);
    }

    private Integer readEmbeddingDims(String indexName) {
        try {
            var mappingResponse = esClient.indices().getMapping(m -> m.index(indexName));
            var mapping = mappingResponse.get(indexName);
            if (mapping != null && mapping.mappings() != null && mapping.mappings().properties() != null) {
                var embeddingProp = mapping.mappings().properties().get("embedding");
                if (embeddingProp != null && embeddingProp.denseVector() != null) {
                    return embeddingProp.denseVector().dims();
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    /**
     * 索引健康状态
     */
    public static class IndexHealth {
        private String indexName;
        private boolean healthy;
        private boolean esConnected;
        private boolean indexExists;
        private long documentCount;  // chunks 总数
        private long articleCount;   // 去重后的文章数量
        private String message;
        
        // Getters and Setters
        public String getIndexName() { return indexName; }
        public void setIndexName(String indexName) { this.indexName = indexName; }
        
        public boolean isHealthy() { return healthy; }
        public void setHealthy(boolean healthy) { this.healthy = healthy; }
        
        public boolean isEsConnected() { return esConnected; }
        public void setEsConnected(boolean esConnected) { this.esConnected = esConnected; }
        
        public boolean isIndexExists() { return indexExists; }
        public void setIndexExists(boolean indexExists) { this.indexExists = indexExists; }
        
        public long getDocumentCount() { return documentCount; }
        public void setDocumentCount(long documentCount) { this.documentCount = documentCount; }
        
        public long getArticleCount() { return articleCount; }
        public void setArticleCount(long articleCount) { this.articleCount = articleCount; }
        
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
    }
}
