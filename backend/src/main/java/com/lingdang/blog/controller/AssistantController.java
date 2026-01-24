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
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.concurrent.Executor;

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
    
    @Autowired
    @Qualifier("sseTaskExecutor")
    private Executor sseTaskExecutor;
    
    /**
     * 流式查询（SSE）
     * 
     * 使用线程池管理异步任务，避免 new Thread() 造成的资源浪费
     * SseEmitter 本身是异步的，不会阻塞 Servlet 线程
     */
    @PostMapping(value = "/query/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter queryStream(
            @Valid @RequestBody AssistantRequest request,
            HttpServletRequest httpRequest) {
        
        SseEmitter emitter = new SseEmitter(120000L); // 2分钟超时
        String clientIp = getClientIp(httpRequest);
        
        // 限流检查
        if (!rateLimitService.allowRequest(clientIp)) {
            try {
                emitter.send(SseEmitter.event()
                    .name("error")
                    .data("{\"message\":\"请求过于频繁，请稍后再试\"}"));
                emitter.complete();
            } catch (Exception e) {
                log.error("发送限流错误失败", e);
            }
            return emitter;
        }
        
        // 使用线程池执行异步查询（替代 new Thread()）
        sseTaskExecutor.execute(() -> {
            try {
                ragService.queryStream(request, clientIp, emitter);
            } catch (Exception e) {
                log.error("流式查询异常", e);
                try {
                    emitter.send(SseEmitter.event()
                        .name("error")
                        .data("{\"message\":\"" + e.getMessage() + "\"}"));
                    emitter.complete();
                } catch (Exception ex) {
                    log.error("发送错误失败", ex);
                }
            }
        });
        
        return emitter;
    }
    
    /**
     * 非流式查询（兼容旧版本）
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
