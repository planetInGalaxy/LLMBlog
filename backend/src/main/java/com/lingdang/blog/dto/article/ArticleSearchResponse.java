package com.lingdang.blog.dto.article;

import lombok.Data;

import java.util.List;

@Data
public class ArticleSearchResponse {
    private List<ArticleSearchItemDTO> items;
    private long total;
    private int page;
    private int pageSize;
}
