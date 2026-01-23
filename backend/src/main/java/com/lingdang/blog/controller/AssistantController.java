package com.lingdang.blog.controller;

import com.lingdang.blog.dto.ApiResponse;
import com.lingdang.blog.dto.assistant.AssistantRequest;
import com.lingdang.blog.dto.assistant.AssistantResponse;
import com.lingdang.blog.service.RagService;
import com.lingdang.blog.service.RateLimitService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Assistant API Controller
 */
@Slf4j
@RestController
@RequestMapping("/api/assistant")
@CrossOrigin(origins = "*")
public class AssistantController {
    
    @Autowired
    private RagService ragService;
    
    @Autowired
    private RateLimitService rateLimitService;
    
    /**
     * 查询
     */
    @PostMapping("/query")
    public ResponseEntity<ApiResponse<AssistantResponse>> query(
            @Valid @RequestBody AssistantRequest request,
            HttpServletRequest httpRequest) {
        
        String clientIp = getClientIp(httpRequest);
        
        // 限流检查
        if (!rateLimitService.allowRequest(clientIp)) {
            return ResponseEntity.ok(ApiResponse.error("请求过于频繁，请稍后再试"));
        }
        
        try {
            AssistantResponse response = ragService.query(request, clientIp);
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (Exception e) {
            log.error("Assistant 查询失败", e);
            return ResponseEntity.ok(ApiResponse.error("查询失败: " + e.getMessage()));
        }
    }
    
    /**
     * 获取客户端 IP
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }
}
