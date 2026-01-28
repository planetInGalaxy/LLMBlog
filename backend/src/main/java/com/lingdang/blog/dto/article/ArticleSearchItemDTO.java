package com.lingdang.blog.dto.article;

import lombok.Data;

/**
 * 文章搜索结果条目：在 ArticleDTO 基础上增加 snippet（来自 ES 高亮）。
 */
@Data
public class ArticleSearchItemDTO extends ArticleDTO {

    /**
     * 命中片段（HTML，通常包含 <em> 高亮标签）
     */
    private String snippet;
}
