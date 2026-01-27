package com.lingdang.blog.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.bulk.BulkOperation;
import com.lingdang.blog.config.ElasticsearchInitializer;
import com.lingdang.blog.model.*;
import com.lingdang.blog.repository.ArticleRepository;
import com.lingdang.blog.repository.elasticsearch.ChunkDocumentRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 全量重建索引（蓝绿：新建索引 -> 写入 -> 切换 alias）
 *
 * 目标：当 chunkSize 等“会影响索引结构/内容”的配置变更时，保证：
 * - 重建失败不影响线上查询（alias 仍指向旧索引）
 * - 重建成功后才切换 alias
 */
@Slf4j
@Service
public class FullReindexService {

    @Autowired
    private ArticleRepository articleRepository;

    @Autowired
    private ChunkService chunkService;

    @Autowired
    private LlmService llmService;

    @Autowired
    private ElasticsearchClient esClient;

    @Autowired
    private ElasticsearchInitializer esInitializer;

    // 仅用于一些 deleteByArticleId 之类的旧逻辑；全量重建写入不走 repository
    @Autowired
    private ChunkDocumentRepository chunkDocumentRepository;

    public interface ProgressListener {
        void onProgress(int totalArticles, int doneArticles);
    }

    public interface NewIndexListener {
        void onNewIndexCreated(String newIndex);
    }

    public void rebuildAllPublishedToNewIndex(ChunkingOptions options) {
        rebuildAllPublishedToNewIndex(options, null, null);
    }

    public void rebuildAllPublishedToNewIndex(ChunkingOptions options, ProgressListener progressListener, NewIndexListener newIndexListener) {
        String oldIndex = esInitializer.resolveCurrentIndex();
        String newIndex = null;

        try {
            // 1) 创建新索引
            newIndex = esInitializer.createNewConcreteIndex();
            if (newIndexListener != null) {
                newIndexListener.onNewIndexCreated(newIndex);
            }

            // 2) 写入新索引
            List<Article> articles = articleRepository.findByStatusOrderByPublishedAtDesc(ArticleStatus.PUBLISHED);
            log.info("开始全量重建索引: articles={}, oldIndex={}, newIndex={}", articles.size(), oldIndex, newIndex);

            int totalChunks = 0;
            int doneArticles = 0;
            for (Article article : articles) {
                List<ArticleChunk> chunks = chunkService.splitArticle(article, options);
                totalChunks += chunks.size();

                // 批量生成 embedding + bulk 写 ES
                bulkIndexChunks(newIndex, chunks);

                doneArticles++;
                if (progressListener != null) {
                    progressListener.onProgress(articles.size(), doneArticles);
                }
            }

            log.info("全量重建写入完成: newIndex={}, chunks={}", newIndex, totalChunks);

            // 3) 切换 alias
            esInitializer.switchAliasTo(newIndex);

            // 4) 可选：删除旧索引（保守起见这里不删，避免误删；你确认后我可以加一个保留 N 个索引的清理策略）
            log.info("全量重建完成并切换 alias 成功: alias={}, newIndex={}, oldIndex={}",
                ElasticsearchInitializer.INDEX_ALIAS, newIndex, oldIndex);

        } catch (Exception e) {
            log.error("全量重建索引失败，将回滚并保持旧索引不变: oldIndex={}, newIndex={}", oldIndex, newIndex, e);

            // 失败时删除新索引（如果已创建）
            if (newIndex != null) {
                try {
                    final String indexToDelete = newIndex;
                    esClient.indices().delete(d -> d.index(indexToDelete));
                } catch (Exception ignored) {
                }
            }

            // alias 未切换则线上不受影响；若切换后才失败（极少），理论上需要切回 oldIndex。
            // 当前 switchAliasTo 发生在最后，因此这里通常不需要额外回滚。

            throw new RuntimeException("全量重建索引失败: " + e.getMessage(), e);
        }
    }

    private void bulkIndexChunks(String targetIndex, List<ArticleChunk> chunks) throws Exception {
        if (chunks == null || chunks.isEmpty()) return;

        List<BulkOperation> ops = new ArrayList<>();

        for (ArticleChunk chunk : chunks) {
            float[] embedding = llmService.generateEmbedding(chunk.getChunkText());

            ChunkDocument doc = new ChunkDocument();
            doc.setChunkId(chunk.getChunkId());
            doc.setArticleId(chunk.getArticleId());
            doc.setSlug(chunk.getSlug());
            doc.setTitle(chunk.getTitle());
            doc.setTags(chunk.getTags());
            doc.setStatus(chunk.getStatus().name());
            doc.setIndexVersion(chunk.getIndexVersion());
            doc.setHeadingLevel(chunk.getHeadingLevel());
            doc.setHeadingText(chunk.getHeadingText());
            doc.setAnchor(chunk.getAnchor());
            doc.setChunkText(chunk.getChunkText());
            doc.setEmbedding(embedding);
            doc.setTokenCount(chunk.getTokenCount());
            doc.setSequenceNumber(chunk.getSequenceNumber());

            ops.add(BulkOperation.of(b -> b.index(i -> i
                .index(targetIndex)
                .id(doc.getChunkId())
                .document(doc)
            )));
        }

        BulkRequest request = BulkRequest.of(b -> b.operations(ops));
        var resp = esClient.bulk(request);
        if (resp.errors()) {
            throw new RuntimeException("bulk 写入 ES 失败: " + resp.items().stream().filter(i -> i.error() != null).findFirst().map(i -> i.error().reason()).orElse("unknown"));
        }
    }
}
