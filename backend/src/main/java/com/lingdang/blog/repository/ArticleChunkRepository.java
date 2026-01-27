package com.lingdang.blog.repository;

import com.lingdang.blog.model.ArticleChunk;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 文章片段数据访问层
 */
@Repository
public interface ArticleChunkRepository extends JpaRepository<ArticleChunk, Long> {
    
    /**
     * 根据 chunk_id 查找
     */
    Optional<ArticleChunk> findByChunkId(String chunkId);
    
    /**
     * 根据文章 ID 查找所有 chunk
     */
    List<ArticleChunk> findByArticleIdOrderBySequenceNumberAsc(Long articleId);

    /**
     * 分页查询 chunks（可选按 articleId 过滤）
     */
    Page<ArticleChunk> findByArticleId(Long articleId, Pageable pageable);
    
    /**
     * 根据文章 ID 和索引版本查找
     */
    List<ArticleChunk> findByArticleIdAndIndexVersion(Long articleId, Integer indexVersion);
    
    /**
     * 删除指定文章的所有 chunk
     */
    void deleteByArticleId(Long articleId);
    
    /**
     * 删除指定文章的旧版本 chunk
     */
    void deleteByArticleIdAndIndexVersionLessThan(Long articleId, Integer indexVersion);
}
