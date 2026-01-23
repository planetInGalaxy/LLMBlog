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
            log.info("检查 Elasticsearch 索引: {}", INDEX_NAME);
            
            // 检查索引是否存在
            ExistsRequest existsRequest = ExistsRequest.of(e -> e.index(INDEX_NAME));
            boolean exists = esClient.indices().exists(existsRequest).value();
            
            if (exists) {
                log.info("索引已存在: {}", INDEX_NAME);
                return;
            }
            
            log.info("索引不存在，开始创建: {}", INDEX_NAME);
            
            // 读取索引配置文件
            ClassPathResource resource = new ClassPathResource("elasticsearch/chunk-settings.json");
            String settings;
            try (InputStream is = resource.getInputStream()) {
                settings = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            }
            
            // 创建索引
            CreateIndexRequest request = CreateIndexRequest.of(c -> c
                .index(INDEX_NAME)
                .withJson(new java.io.StringReader(settings))
            );
            
            esClient.indices().create(request);
            log.info("✅ 索引创建成功: {}", INDEX_NAME);
            
        } catch (Exception e) {
            log.error("❌ 索引初始化失败: {}", e.getMessage(), e);
            log.warn("索引初始化失败不影响应用启动，可在后续手动创建");
        }
    }
}
