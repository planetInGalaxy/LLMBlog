package com.lingdang.blog.repository;

import com.lingdang.blog.model.IndexJobStatus;
import com.lingdang.blog.model.RagIndexJob;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * RAG 索引任务数据访问层
 */
@Repository
public interface RagIndexJobRepository extends JpaRepository<RagIndexJob, Long> {
    
    /**
     * 查找指定文章正在运行的任务
     */
    Optional<RagIndexJob> findFirstByArticleIdAndStatus(Long articleId, IndexJobStatus status);
    
    /**
     * 查找指定文章的所有任务
     */
    List<RagIndexJob> findByArticleIdOrderByCreatedAtDesc(Long articleId);
    
    /**
     * 查找指定状态的任务
     */
    List<RagIndexJob> findByStatusOrderByCreatedAtAsc(IndexJobStatus status);
    
    /**
     * 查找最近的任务
     */
    List<RagIndexJob> findTop100ByOrderByCreatedAtDesc();
}
