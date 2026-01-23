package com.lingdang.blog.dto.assistant;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Assistant 查询请求
 */
@Data
public class AssistantRequest {
    
    @NotBlank(message = "问题不能为空")
    private String question;
    
    private String mode = "ARTICLE_ONLY";
}
