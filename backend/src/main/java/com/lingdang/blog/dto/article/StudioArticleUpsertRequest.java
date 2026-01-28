package com.lingdang.blog.dto.article;

import lombok.Data;

/**
 * Studio 文章创建/更新请求。
 * - 复用 ArticleDTO 的字段结构
 * - 额外提供 regenerateSummary 开关：保存后是否重生成摘要
 */
@Data
public class StudioArticleUpsertRequest extends ArticleDTO {

    /**
     * 保存后是否重新生成摘要（显式开关）。
     * - true：创建 REGENERATE 任务，允许覆盖现有 summary
     * - false/null：仅当 summary 为空时补齐
     */
    private Boolean regenerateSummary;
}
