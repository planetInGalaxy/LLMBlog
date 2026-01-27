package com.lingdang.blog.service;

import com.lingdang.blog.dto.article.ArticleDTO;
import com.lingdang.blog.model.Article;
import com.lingdang.blog.model.ArticleStatus;
import com.lingdang.blog.repository.ArticleRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 文章服务
 */
@Slf4j
@Service
@Transactional
public class ArticleService {

    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
        "updatedAt",
        "publishedAt",
        "createdAt",
        "title",
        "viewCount",
        "id"
    );

    @Autowired
    private ArticleRepository articleRepository;
    
    /**
     * 获取所有文章（包含草稿）
     */
    public List<ArticleDTO> getAllArticles() {
        List<Article> articles = articleRepository.findByStatusInOrderByUpdatedAtDesc(
            List.of(ArticleStatus.DRAFT, ArticleStatus.PUBLISHED, ArticleStatus.OFFLINE)
        );
        return articles.stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    /**
     * 获取所有文章（包含草稿）- 支持分页/排序
     */
    public List<ArticleDTO> getAllArticles(Integer page, Integer pageSize, String sortBy, String sortOrder) {
        if (page == null && pageSize == null && sortBy == null && sortOrder == null) {
            return getAllArticles();
        }
        Pageable pageable = buildPageable(page, pageSize, sortBy, sortOrder, "updatedAt");
        Page<Article> pageResult = articleRepository.findByStatusIn(
            List.of(ArticleStatus.DRAFT, ArticleStatus.PUBLISHED, ArticleStatus.OFFLINE),
            pageable
        );
        return pageResult.getContent().stream().map(this::convertToDTO).collect(Collectors.toList());
    }
    
    /**
     * 获取已发布文章
     */
    public List<ArticleDTO> getPublishedArticles() {
        List<Article> articles = articleRepository.findByStatusOrderByPublishedAtDesc(ArticleStatus.PUBLISHED);
        return articles.stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    /**
     * 获取已发布文章 - 支持分页/排序
     */
    public List<ArticleDTO> getPublishedArticles(Integer page, Integer pageSize, String sortBy, String sortOrder) {
        if (page == null && pageSize == null && sortBy == null && sortOrder == null) {
            return getPublishedArticles();
        }
        Pageable pageable = buildPageable(page, pageSize, sortBy, sortOrder, "publishedAt");
        Page<Article> pageResult = articleRepository.findByStatus(ArticleStatus.PUBLISHED, pageable);
        return pageResult.getContent().stream().map(this::convertToDTO).collect(Collectors.toList());
    }
    
    /**
     * 根据 ID 获取文章
     */
    public Optional<ArticleDTO> getArticleById(Long id) {
        return articleRepository.findById(id).map(this::convertToDTO);
    }
    
    /**
     * 根据 Slug 获取文章
     */
    public Optional<ArticleDTO> getArticleBySlug(String slug) {
        return articleRepository.findBySlug(slug).map(this::convertToDTO);
    }
    
    /**
     * 创建文章（草稿）
     */
    public ArticleDTO createArticle(ArticleDTO dto) {
        // 检查 slug 是否已存在
        if (articleRepository.findBySlug(dto.getSlug()).isPresent()) {
            throw new RuntimeException("Slug 已存在: " + dto.getSlug());
        }
        
        Article article = new Article();
        BeanUtils.copyProperties(dto, article, "id", "createdAt", "updatedAt");
        
        if (article.getStatus() == null) {
            article.setStatus(ArticleStatus.DRAFT);
        }
        if (article.getViewCount() == null) {
            article.setViewCount(0L);
        }
        if (article.getIndexVersion() == null) {
            article.setIndexVersion(0);
        }
        
        Article saved = articleRepository.save(article);
        log.info("创建文章: id={}, title={}, status={}", saved.getId(), saved.getTitle(), saved.getStatus());
        
        return convertToDTO(saved);
    }
    
    /**
     * 更新文章
     */
    public ArticleDTO updateArticle(Long id, ArticleDTO dto) {
        Article article = articleRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("文章不存在: " + id));
        
        // 检查 slug 是否被其他文章占用
        articleRepository.findBySlug(dto.getSlug()).ifPresent(existing -> {
            if (!existing.getId().equals(id)) {
                throw new RuntimeException("Slug 已被其他文章使用: " + dto.getSlug());
            }
        });
        
        // 更新字段
        article.setTitle(dto.getTitle());
        article.setSlug(dto.getSlug());
        article.setSummary(dto.getSummary());
        article.setContentMarkdown(dto.getContentMarkdown());
        article.setAuthor(dto.getAuthor());
        article.setTags(dto.getTags());
        article.setCoverUrl(dto.getCoverUrl());
        
        Article updated = articleRepository.save(article);
        log.info("更新文章: id={}, title={}", updated.getId(), updated.getTitle());
        
        return convertToDTO(updated);
    }
    
    /**
     * 发布文章
     */
    public ArticleDTO publishArticle(Long id) {
        Article article = articleRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("文章不存在: " + id));
        
        article.setStatus(ArticleStatus.PUBLISHED);
        article.setPublishedAt(LocalDateTime.now());
        
        Article published = articleRepository.save(article);
        log.info("发布文章: id={}, title={}", published.getId(), published.getTitle());
        
        return convertToDTO(published);
    }
    
    /**
     * 下线文章
     */
    public ArticleDTO offlineArticle(Long id) {
        Article article = articleRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("文章不存在: " + id));
        
        article.setStatus(ArticleStatus.OFFLINE);
        
        Article offline = articleRepository.save(article);
        log.info("下线文章: id={}, title={}", offline.getId(), offline.getTitle());
        
        return convertToDTO(offline);
    }
    
    /**
     * 删除文章
     */
    public void deleteArticle(Long id) {
        if (!articleRepository.existsById(id)) {
            throw new RuntimeException("文章不存在: " + id);
        }
        
        articleRepository.deleteById(id);
        log.info("删除文章: id={}", id);
    }
    
    /**
     * 增加浏览次数
     */
    public void incrementViewCount(Long id) {
        articleRepository.findById(id).ifPresent(article -> {
            article.setViewCount(article.getViewCount() + 1);
            articleRepository.save(article);
        });
    }
    
    /**
     * 搜索文章
     */
    public List<ArticleDTO> searchArticles(String keyword) {
        List<Article> articles = articleRepository.findByStatusAndTitleContainingIgnoreCaseOrderByPublishedAtDesc(
            ArticleStatus.PUBLISHED, keyword
        );
        return articles.stream().map(this::convertToDTO).collect(Collectors.toList());
    }

    private Pageable buildPageable(Integer page, Integer pageSize, String sortBy, String sortOrder, String defaultSortBy) {
        int resolvedPage = (page == null || page < 1) ? 1 : page;
        int resolvedPageSize = (pageSize == null || pageSize < 1) ? DEFAULT_PAGE_SIZE : pageSize;
        String resolvedSortBy = (sortBy != null && ALLOWED_SORT_FIELDS.contains(sortBy)) ? sortBy : defaultSortBy;
        Sort.Direction direction = "asc".equalsIgnoreCase(sortOrder) ? Sort.Direction.ASC : Sort.Direction.DESC;
        return PageRequest.of(resolvedPage - 1, resolvedPageSize, Sort.by(direction, resolvedSortBy));
    }
    
    /**
     * 转换为 DTO
     */
    private ArticleDTO convertToDTO(Article article) {
        ArticleDTO dto = new ArticleDTO();
        BeanUtils.copyProperties(article, dto);
        return dto;
    }
}
