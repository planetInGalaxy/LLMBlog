package com.lingdang.blog.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.HashMap;
import java.util.Map;

/**
 * 健康检查 Controller
 */
@RestController
@RequestMapping("/api/health")
public class HealthController {
    
    @Autowired
    private DataSource dataSource;
    
    @Autowired(required = false)
    private ElasticsearchTemplate elasticsearchTemplate;
    
    @GetMapping
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("service", "lingdang-blog-backend");
        
        // 检查数据库连接
        try (Connection conn = dataSource.getConnection()) {
            health.put("database", "UP");
        } catch (Exception e) {
            health.put("database", "DOWN: " + e.getMessage());
        }
        
        // 检查 ES 连接
        if (elasticsearchTemplate != null) {
            try {
                // 简单检查（不执行实际操作，避免索引不存在时报错）
                health.put("elasticsearch", "CONFIGURED");
            } catch (Exception e) {
                health.put("elasticsearch", "DOWN: " + e.getMessage());
            }
        } else {
            health.put("elasticsearch", "NOT_CONFIGURED");
        }
        
        return ResponseEntity.ok(health);
    }
}
