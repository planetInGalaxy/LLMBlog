package com.lingdang.blog.dto.assistant;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

/**
 * Assistant 查询请求
 */
@Data
public class AssistantRequest {
    
    @NotBlank(message = "问题不能为空")
    private String question;
    
    // ARTICLE_ONLY: 只基于文章回答
    // FLEXIBLE: 灵活模式，有文章就引用，没有就直接回答
    private String mode = "FLEXIBLE";
    
    /**
     * 历史对话消息（用于多轮对话）
     * 格式：[{role: "user", content: "..."}, {role: "assistant", content: "..."}]
     */
    private List<ChatMessage> history;
    
    @Data
    public static class ChatMessage {
        private String role;     // "user" 或 "assistant"
        private String content;  // 消息内容
    }
}
