package com.lingdang.blog.dto.llm;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Embedding API 请求
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmbeddingRequest {
    
    /**
     * 模型名称
     */
    private String model;
    
    /**
     * 输入文本（可以是单个字符串或字符串数组）
     */
    private List<String> input;
    
    /**
     * 编码格式
     */
    @JsonProperty("encoding_format")
    private String encodingFormat = "float";
}
