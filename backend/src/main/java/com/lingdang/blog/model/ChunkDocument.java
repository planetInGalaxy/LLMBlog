package com.lingdang.blog.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

/**
 * Elasticsearch 文档实体（对应索引中的 chunk 文档）
 * 
 * 注意：索引会由 ElasticsearchInitializer 在应用启动时自动创建
 * 不使用 @Setting 注解，避免与手动创建索引冲突
 * 
 * @JsonIgnoreProperties(ignoreUnknown = true) 用于忽略 ES 自动添加的 _class 字段
 */
@Data
@Document(indexName = "lingdang_chunks")
@JsonIgnoreProperties(ignoreUnknown = true)
public class ChunkDocument {
    
    /**
     * Chunk 唯一标识符
     */
    @Id
    private String chunkId;
    
    /**
     * 关联的文章 ID
     */
    @Field(type = FieldType.Long)
    private Long articleId;
    
    /**
     * 文章 slug
     */
    @Field(type = FieldType.Keyword)
    private String slug;
    
    /**
     * 文章标题
     */
    @Field(type = FieldType.Text, analyzer = "ik_max_word")
    private String title;
    
    /**
     * 标签
     */
    @Field(type = FieldType.Text, analyzer = "ik_max_word")
    private String tags;
    
    /**
     * 文章状态
     */
    @Field(type = FieldType.Keyword)
    private String status;
    
    /**
     * 索引版本号
     */
    @Field(type = FieldType.Integer)
    private Integer indexVersion;
    
    /**
     * 标题层级
     */
    @Field(type = FieldType.Integer)
    private Integer headingLevel;
    
    /**
     * 标题文本
     */
    @Field(type = FieldType.Text, analyzer = "ik_max_word")
    private String headingText;
    
    /**
     * 锚点
     */
    @Field(type = FieldType.Keyword)
    private String anchor;
    
    /**
     * Chunk 文本内容
     */
    @Field(type = FieldType.Text, analyzer = "ik_max_word")
    private String chunkText;
    
    /**
     * 向量嵌入（dense_vector）
     * 维度说明：
     * - OpenAI text-embedding-3-small: 1536 维
     * - Ollama nomic-embed-text: 768 维
     * - Ollama mxbai-embed-large: 1024 维
     * 根据实际使用的 embedding 模型配置维度
     */
    @Field(type = FieldType.Dense_Vector, dims = 768)
    private float[] embedding;
    
    /**
     * Token 数量
     */
    @Field(type = FieldType.Integer)
    private Integer tokenCount;
    
    /**
     * Chunk 序号
     */
    @Field(type = FieldType.Integer)
    private Integer sequenceNumber;
}
