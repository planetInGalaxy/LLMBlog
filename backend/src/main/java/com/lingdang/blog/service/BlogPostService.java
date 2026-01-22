package com.lingdang.blog.service;

import com.lingdang.blog.model.BlogPost;
import com.lingdang.blog.repository.BlogPostRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * 博客文章服务层
 */
@Service
@Transactional
public class BlogPostService {
    
    @Autowired
    private BlogPostRepository blogPostRepository;
    
    /**
     * 获取所有已发布的文章
     */
    public List<BlogPost> getAllPublishedPosts() {
        return blogPostRepository.findByPublishedTrueOrderByCreatedAtDesc();
    }
    
    /**
     * 根据ID获取文章
     */
    public Optional<BlogPost> getPostById(Long id) {
        return blogPostRepository.findById(id);
    }
    
    /**
     * 创建新文章
     */
    public BlogPost createPost(BlogPost post) {
        return blogPostRepository.save(post);
    }
    
    /**
     * 更新文章
     */
    public BlogPost updatePost(Long id, BlogPost updatedPost) {
        return blogPostRepository.findById(id)
            .map(post -> {
                post.setTitle(updatedPost.getTitle());
                post.setContent(updatedPost.getContent());
                post.setAuthor(updatedPost.getAuthor());
                post.setSummary(updatedPost.getSummary());
                post.setTags(updatedPost.getTags());
                post.setPublished(updatedPost.getPublished());
                return blogPostRepository.save(post);
            })
            .orElseThrow(() -> new RuntimeException("文章未找到，ID: " + id));
    }
    
    /**
     * 删除文章
     */
    public void deletePost(Long id) {
        blogPostRepository.deleteById(id);
    }
    
    /**
     * 增加文章浏览次数
     */
    public void incrementViewCount(Long id) {
        blogPostRepository.findById(id).ifPresent(post -> {
            post.setViewCount(post.getViewCount() + 1);
            blogPostRepository.save(post);
        });
    }
    
    /**
     * 搜索文章
     */
    public List<BlogPost> searchPosts(String keyword) {
        return blogPostRepository.findByTitleContainingIgnoreCaseOrderByCreatedAtDesc(keyword);
    }
}
