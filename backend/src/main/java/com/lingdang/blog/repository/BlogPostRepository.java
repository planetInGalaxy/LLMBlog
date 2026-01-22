package com.lingdang.blog.repository;

import com.lingdang.blog.model.BlogPost;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 博客文章数据访问层
 */
@Repository
public interface BlogPostRepository extends JpaRepository<BlogPost, Long> {
    
    /**
     * 查找已发布的文章
     */
    List<BlogPost> findByPublishedTrueOrderByCreatedAtDesc();
    
    /**
     * 根据标题搜索文章
     */
    List<BlogPost> findByTitleContainingIgnoreCaseOrderByCreatedAtDesc(String title);
}
