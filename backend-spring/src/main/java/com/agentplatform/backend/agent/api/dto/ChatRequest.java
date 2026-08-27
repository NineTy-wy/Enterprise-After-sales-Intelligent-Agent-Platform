package com.agentplatform.backend.agent.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * 对外 Agent 问答请求。
 */
public record ChatRequest(
        String sessionId,
        @NotBlank(message = "问题不能为空")
        @Size(max = 4000, message = "问题不能超过 4000 个字符")
        String query,
        List<String> knowledgeBaseIds,
        String mode
) {
}
