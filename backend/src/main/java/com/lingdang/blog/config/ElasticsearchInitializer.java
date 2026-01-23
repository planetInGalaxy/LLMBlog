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
    
    private static final String INDEX_NAME = "lingdang_chunks_v1";
    
    /**
     * 应用就绪后初始化索引
     */
    @EventListener(ApplicationReadyEvent.class)
    public void initializeIndex() {
        try {
            log.info("=== Elasticsearch 索引初始化开始 ===");
            log.info("目标索引: {}", INDEX_NAME);
            
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
            
            // 检查索引是否存在
            ExistsRequest existsRequest = ExistsRequest.of(e -> e.index(INDEX_NAME));
            boolean exists = esClient.indices().exists(existsRequest).value();
            
            if (exists) {
                log.info("✅ 索引已存在: {}", INDEX_NAME);
                
                // 检查 embedding 维度是否匹配（自动修复维度不匹配问题）
                try {
                    var mappingResponse = esClient.indices().getMapping(m -> m.index(INDEX_NAME));
                    var mapping = mappingResponse.get(INDEX_NAME);
                    if (mapping != null && mapping.mappings() != null && mapping.mappings().properties() != null) {
                        var embeddingProp = mapping.mappings().properties().get("embedding");
                        if (embeddingProp != null && embeddingProp._kind() != null) {
                            // 获取当前索引中 embedding 的维度
                            var denseVector = embeddingProp.denseVector();
                            if (denseVector != null) {
                                Integer dimsValue = denseVector.dims();
                                if (dimsValue != null) {
                                    int currentDims = dimsValue;
                                    int expectedDims = 768; // 与 ChunkDocument 中定义的维度一致
                                    
                                    if (currentDims != expectedDims) {
                                        log.warn("⚠️  检测到 embedding 维度不匹配！");
                                        log.warn("    当前索引维度: {}", currentDims);
                                        log.warn("    期望的维度: {}", expectedDims);
                                        log.warn("    自动删除旧索引并重建...");
                                        
                                        // 删除旧索引
                                        esClient.indices().delete(d -> d.index(INDEX_NAME));
                                        log.info("✅ 已删除旧索引");
                                        
                                        // 跳转到创建索引逻辑
                                        exists = false;
                                    } else {
                                        log.info("✅ Embedding 维度匹配: {} 维", currentDims);
                                    }
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    log.warn("检查 embedding 维度失败（将继续使用现有索引）: {}", e.getMessage());
                }
                
                if (exists) {
                    // 获取索引文档数量
                    long count = esClient.count(c -> c.index(INDEX_NAME)).count();
                    log.info("📊 索引文档数量: {}", count);
                    return;
                }
            }
            
            log.info("索引不存在，Spring Data Elasticsearch 将自动创建");
            log.info("提示：索引会在首次使用 ChunkDocumentRepository 时自动创建");
            log.info("提示：请在 Studio 执行「全量重建索引」来触发索引创建和数据导入");
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
        health.setIndexName(INDEX_NAME);
        
        try {
            // 检查 ES 连接
            boolean pingResult = esClient.ping().value();
            health.setEsConnected(pingResult);
            
            if (!pingResult) {
                health.setHealthy(false);
                health.setMessage("Elasticsearch 连接失败");
                return health;
            }
            
            // 检查索引是否存在
            boolean exists = esClient.indices().exists(e -> e.index(INDEX_NAME)).value();
            health.setIndexExists(exists);
            
            if (!exists) {
                health.setHealthy(false);
                health.setMessage("索引不存在，请执行全量重建索引");
                return health;
            }
            
            // 获取文档数量
            long count = esClient.count(c -> c.index(INDEX_NAME)).count();
            health.setDocumentCount(count);
            
            health.setHealthy(true);
            health.setMessage("索引健康");
            
        } catch (Exception e) {
            health.setHealthy(false);
            health.setMessage("检查失败: " + e.getMessage());
        }
        
        return health;
    }
    
    /**
     * 索引健康状态
     */
    public static class IndexHealth {
        private String indexName;
        private boolean healthy;
        private boolean esConnected;
        private boolean indexExists;
        private long documentCount;
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
        
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
    }
}
