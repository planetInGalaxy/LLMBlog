package com.lingdang.blog.repository.elasticsearch;

import com.lingdang.blog.model.ChunkDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Chunk 文档 ES Repository
 */
@Repository
public interface ChunkDocumentRepository extends ElasticsearchRepository<ChunkDocument, String> {
    
    /**
     * 根据文章 ID 删除所有 chunk
     */
    void deleteByArticleId(Long articleId);
    
    /**
     * 根据文章 ID 查找所有 chunk
     */
    List<ChunkDocument> findByArticleId(Long articleId);
    
    /**
     * 根据文章 ID 和索引版本查找
     */
    List<ChunkDocument> findByArticleIdAndIndexVersion(Long articleId, Integer indexVersion);
}
