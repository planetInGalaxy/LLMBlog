package com.lingdang.blog.repository;

import com.lingdang.blog.model.RagQueryHit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface RagQueryHitRepository extends JpaRepository<RagQueryHit, Long> {
    List<RagQueryHit> findByRequestIdOrderByRankNoAsc(String requestId);

    long deleteByCreatedAtBefore(LocalDateTime before);
}
