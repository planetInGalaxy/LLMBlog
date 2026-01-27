package com.lingdang.blog.controller;

import com.lingdang.blog.config.ElasticsearchInitializer;
import com.lingdang.blog.dto.ApiResponse;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.time.Instant;

/**
 * 健康检查 Controller
 */
@RestController
@RequestMapping("/api/health")
public class HealthController {
    
    @Autowired
    private DataSource dataSource;

    @Autowired
    private ElasticsearchInitializer esInitializer;

    @GetMapping
    public ResponseEntity<ApiResponse<HealthStatus>> health() {
        HealthStatus health = new HealthStatus();
        health.setStatus("UP");
        health.setService("lingdang-blog-backend");
        health.setAlive(true);
        health.setTimestamp(Instant.now().toString());

        DatabaseHealth database = new DatabaseHealth();
        try (Connection conn = dataSource.getConnection()) {
            database.setConnected(true);
            database.setMessage("OK");
        } catch (Exception e) {
            database.setConnected(false);
            database.setMessage(e.getMessage());
        }
        health.setDatabase(database);

        try {
            health.setIndexHealth(esInitializer.checkIndexHealth());
        } catch (Exception e) {
            ElasticsearchInitializer.IndexHealth fallback = new ElasticsearchInitializer.IndexHealth();
            fallback.setHealthy(false);
            fallback.setEsConnected(false);
            fallback.setIndexExists(false);
            fallback.setMessage("检查失败: " + e.getMessage());
            health.setIndexHealth(fallback);
        }

        return ResponseEntity.ok(ApiResponse.success(health));
    }

    @Data
    public static class HealthStatus {
        private String status;
        private String service;
        private boolean alive;
        private String timestamp;
        private DatabaseHealth database;
        private ElasticsearchInitializer.IndexHealth indexHealth;
    }

    @Data
    public static class DatabaseHealth {
        private boolean connected;
        private String message;
    }
}
