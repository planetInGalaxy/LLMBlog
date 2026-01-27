package com.lingdang.blog.repository;

import com.lingdang.blog.model.RagConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RagConfigRepository extends JpaRepository<RagConfig, Long> {
}
