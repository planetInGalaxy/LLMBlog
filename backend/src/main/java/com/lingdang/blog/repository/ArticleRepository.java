package com.lingdang.blog.repository;

import com.lingdang.blog.model.Article;
import com.lingdang.blog.model.ArticleStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 文章数据访问层
 */
@Repository
public interface ArticleRepository extends JpaRepository<Article, Long> {
    
    /**
     * 根据 slug 查找文章
     */
    Optional<Article> findBySlug(String slug);
    
    /**
     * 查找已发布的文章
     */
    List<Article> findByStatusOrderByPublishedAtDesc(ArticleStatus status);
    
    /**
     * 根据状态查找文章
     */
    List<Article> findByStatusInOrderByUpdatedAtDesc(List<ArticleStatus> statuses);
    
    /**
     * 根据标题搜索已发布的文章
     */
    List<Article> findByStatusAndTitleContainingIgnoreCaseOrderByPublishedAtDesc(
        ArticleStatus status, String keyword);
}
