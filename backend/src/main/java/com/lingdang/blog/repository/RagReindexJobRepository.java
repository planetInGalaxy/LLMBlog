package com.lingdang.blog.repository;

import com.lingdang.blog.model.RagReindexJob;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface RagReindexJobRepository extends JpaRepository<RagReindexJob, Long> {

    Optional<RagReindexJob> findFirstByOrderByCreatedAtDesc();

    List<RagReindexJob> findByCreatedAtAfterOrderByCreatedAtDesc(LocalDateTime after);

    long deleteByCreatedAtBefore(LocalDateTime before);
}
