package com.lingdang.blog.service;

import com.lingdang.blog.dto.article.ArticleChunkDTO;
import com.lingdang.blog.model.ArticleChunk;
import com.lingdang.blog.repository.ArticleChunkRepository;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ArticleChunkService {

    private static final int DEFAULT_PAGE_SIZE = 20;

    @Autowired
    private ArticleChunkRepository articleChunkRepository;

    public List<ArticleChunkDTO> listChunks(Long articleId, Integer page, Integer pageSize) {
        int resolvedPage = (page == null || page < 1) ? 1 : page;
        int resolvedPageSize = (pageSize == null || pageSize < 1) ? DEFAULT_PAGE_SIZE : pageSize;

        Pageable pageable = PageRequest.of(
            resolvedPage - 1,
            resolvedPageSize,
            Sort.by(Sort.Direction.DESC, "createdAt")
        );

        Page<ArticleChunk> pageResult = (articleId != null)
            ? articleChunkRepository.findByArticleId(articleId, pageable)
            : articleChunkRepository.findAll(pageable);

        return pageResult.getContent().stream().map(this::toDTO).collect(Collectors.toList());
    }

    public List<ArticleChunkDTO> listChunksByArticleId(Long articleId) {
        return articleChunkRepository.findByArticleIdOrderBySequenceNumberAsc(articleId)
            .stream()
            .map(this::toDTO)
            .collect(Collectors.toList());
    }

    private ArticleChunkDTO toDTO(ArticleChunk chunk) {
        ArticleChunkDTO dto = new ArticleChunkDTO();
        BeanUtils.copyProperties(chunk, dto);
        return dto;
    }
}
