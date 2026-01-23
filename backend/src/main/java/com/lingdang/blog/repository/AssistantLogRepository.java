package com.lingdang.blog.repository;

import com.lingdang.blog.model.AssistantLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 助手日志数据访问层
 */
@Repository
public interface AssistantLogRepository extends JpaRepository<AssistantLog, Long> {
    
    /**
     * 根据请求 ID 查找
     */
    Optional<AssistantLog> findByRequestId(String requestId);
    
    /**
     * 查找指定 IP 在指定时间范围内的日志数量（用于限流）
     */
    @Query("SELECT COUNT(a) FROM AssistantLog a WHERE a.clientIp = ?1 AND a.createdAt >= ?2")
    Long countByClientIpAndCreatedAtAfter(String clientIp, LocalDateTime since);
    
    /**
     * 查找最近的日志
     */
    List<AssistantLog> findTop100ByOrderByCreatedAtDesc();
}
