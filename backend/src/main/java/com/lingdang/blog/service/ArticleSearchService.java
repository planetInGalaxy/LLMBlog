package com.lingdang.blog.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch.core.search.InnerHitsResult;
import com.lingdang.blog.config.ElasticsearchInitializer;
import com.lingdang.blog.dto.article.ArticleDTO;
import com.lingdang.blog.dto.article.ArticleSearchItemDTO;
import com.lingdang.blog.dto.article.ArticleSearchResponse;
import com.lingdang.blog.model.Article;
import com.lingdang.blog.model.ArticleStatus;
import com.lingdang.blog.model.ChunkDocument;
import com.lingdang.blog.repository.ArticleRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

import co.elastic.clients.elasticsearch._types.query_dsl.Operator;

@Slf4j
@Service
public class ArticleSearchService {

    private static final int DEFAULT_PAGE_SIZE = 12;

    @Autowired
    private ElasticsearchClient esClient;

    @Autowired
    private ArticleRepository articleRepository;

    public ArticleSearchResponse searchPublished(String q, Integer page, Integer pageSize) {
        String query = q == null ? "" : q.trim();
        int p = (page == null || page < 1) ? 1 : page;
        int ps = (pageSize == null || pageSize < 1 || pageSize > 50) ? DEFAULT_PAGE_SIZE : pageSize;

        ArticleSearchResponse resp = new ArticleSearchResponse();
        resp.setPage(p);
        resp.setPageSize(ps);

        if (query.isEmpty()) {
            resp.setItems(List.of());
            resp.setTotal(0);
            return resp;
        }

        try {
            int from = (p - 1) * ps;

            final int qLen = query.length();

            SearchResponse<ChunkDocument> esResp = esClient.search(s -> s
                    .index(ElasticsearchInitializer.INDEX_ALIAS)
                    .trackTotalHits(t -> t.enabled(true))
                    .from(from)
                    .size(ps)
                    // 只搜已发布
                    .query(qb -> qb.bool(b -> {
                        b.filter(f -> f.term(t -> t.field("status").value(ArticleStatus.PUBLISHED.name())));

                        // ✅ 搜索最佳实践：短查询要更“严格”，避免单字命中导致乱召回
                        if (qLen <= 2) {
                            // 1~2 字：优先短语匹配（phrase），更符合用户直觉
                            b.must(m -> m.bool(bb -> bb
                                .should(s1 -> s1.matchPhrase(mp -> mp.field("title").query(query).boost(5f)))
                                .should(s2 -> s2.matchPhrase(mp -> mp.field("tags").query(query).boost(3f)))
                                .should(s3 -> s3.matchPhrase(mp -> mp.field("chunkText").query(query)))
                                .minimumShouldMatch("1")
                            ));
                        } else if (qLen <= 4) {
                            // 3~4 字：用 AND，要求每个词都命中
                            b.must(m -> m.multiMatch(mm -> mm
                                .query(query)
                                .fields("title^4", "tags^2", "chunkText")
                                .operator(Operator.And)
                            ));
                        } else {
                            // 5 字以上：用 minimum_should_match 控制召回宽松度
                            b.must(m -> m.multiMatch(mm -> mm
                                .query(query)
                                .fields("title^4", "tags^2", "chunkText")
                                .minimumShouldMatch("70%")
                            ));
                        }

                        return b;
                    }))
                    // 以 articleId 折叠，返回文章维度结果
                    .collapse(c -> c
                        .field("articleId")
                        .innerHits(ih -> ih
                            .name("top_chunk")
                            .size(1)
                            .highlight(h -> h
                                .preTags("<em>")
                                .postTags("</em>")
                                .fields("chunkText", f -> f.fragmentSize(120).numberOfFragments(1))
                                .fields("title", f -> f.fragmentSize(80).numberOfFragments(1))
                            )
                            .source(src -> src.filter(f -> f.includes("articleId", "slug", "title", "tags")))
                        )
                    ),
                ChunkDocument.class
            );

            long total = esResp.hits().total() != null ? esResp.hits().total().value() : 0;
            resp.setTotal(total);

            // 取 articleId 顺序（保持 ES 的相关度排序）
            List<Long> articleIds = new ArrayList<>();
            Map<Long, String> snippetByArticleId = new HashMap<>();

            for (Hit<ChunkDocument> hit : esResp.hits().hits()) {
                ChunkDocument src = hit.source();
                if (src == null || src.getArticleId() == null) continue;
                Long articleId = src.getArticleId();
                articleIds.add(articleId);

                // inner_hits -> highlight
                try {
                    Map<String, InnerHitsResult> inner = hit.innerHits();
                    if (inner != null) {
                        InnerHitsResult top = inner.get("top_chunk");
                        if (top != null && top.hits() != null && top.hits().hits() != null && !top.hits().hits().isEmpty()) {
                            var innerHit = top.hits().hits().get(0);
                            if (innerHit.highlight() != null) {
                                List<String> frags = innerHit.highlight().get("chunkText");
                                if (frags == null || frags.isEmpty()) {
                                    frags = innerHit.highlight().get("title");
                                }
                                if (frags != null && !frags.isEmpty()) {
                                    snippetByArticleId.put(articleId, frags.get(0));
                                }
                            }
                        }
                    }
                } catch (Exception ignored) {
                }
            }

            if (articleIds.isEmpty()) {
                resp.setItems(List.of());
                return resp;
            }

            // DB 回表拿完整 ArticleDTO（summary/publishedAt/viewCount 等）
            List<Article> articles = articleRepository.findAllById(articleIds);
            Map<Long, Article> byId = articles.stream().collect(Collectors.toMap(Article::getId, a -> a));

            List<ArticleSearchItemDTO> items = new ArrayList<>();
            for (Long id : articleIds) {
                Article a = byId.get(id);
                if (a == null) continue;
                if (a.getStatus() != ArticleStatus.PUBLISHED) continue;

                ArticleSearchItemDTO item = new ArticleSearchItemDTO();
                BeanUtils.copyProperties(a, item);
                item.setSnippet(snippetByArticleId.get(id));
                items.add(item);
            }

            resp.setItems(items);
            return resp;

        } catch (Exception e) {
            log.error("文章搜索失败: q='{}'", query, e);
            resp.setItems(List.of());
            resp.setTotal(0);
            return resp;
        }
    }
}
