package com.lingdang.blog.repository;

import com.lingdang.blog.model.RagQueryLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface RagQueryLogRepository extends JpaRepository<RagQueryLog, Long> {
    Optional<RagQueryLog> findByRequestId(String requestId);

    List<RagQueryLog> findByCreatedAtAfterOrderByCreatedAtDesc(LocalDateTime after);

    long deleteByCreatedAtBefore(LocalDateTime before);
}
