package com.lingdang.blog.controller;

import com.lingdang.blog.model.BlogPost;
import com.lingdang.blog.service.BlogPostService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 博客文章控制器
 */
@RestController
@RequestMapping("/api/posts")
@CrossOrigin(origins = "*")
public class BlogPostController {
    
    @Autowired
    private BlogPostService blogPostService;
    
    /**
     * 获取所有已发布的文章
     */
    @GetMapping
    public ResponseEntity<List<BlogPost>> getAllPosts() {
        return ResponseEntity.ok(blogPostService.getAllPublishedPosts());
    }
    
    /**
     * 根据ID获取文章
     */
    @GetMapping("/{id}")
    public ResponseEntity<BlogPost> getPostById(@PathVariable Long id) {
        return blogPostService.getPostById(id)
            .map(post -> {
                blogPostService.incrementViewCount(id);
                return ResponseEntity.ok(post);
            })
            .orElse(ResponseEntity.notFound().build());
    }
    
    /**
     * 创建新文章
     */
    @PostMapping
    public ResponseEntity<BlogPost> createPost(@RequestBody BlogPost post) {
        BlogPost createdPost = blogPostService.createPost(post);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdPost);
    }
    
    /**
     * 更新文章
     */
    @PutMapping("/{id}")
    public ResponseEntity<BlogPost> updatePost(
            @PathVariable Long id, 
            @RequestBody BlogPost post) {
        try {
            BlogPost updatedPost = blogPostService.updatePost(id, post);
            return ResponseEntity.ok(updatedPost);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
    
    /**
     * 删除文章
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePost(@PathVariable Long id) {
        blogPostService.deletePost(id);
        return ResponseEntity.noContent().build();
    }
    
    /**
     * 搜索文章
     */
    @GetMapping("/search")
    public ResponseEntity<List<BlogPost>> searchPosts(@RequestParam String keyword) {
        return ResponseEntity.ok(blogPostService.searchPosts(keyword));
    }
}
