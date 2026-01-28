package com.lingdang.blog.dto.studio;

import lombok.Data;

@Data
public class PromptTemplateDTO {
    private String promptKey;
    private String name;
    private String description;
    private String content;
}
