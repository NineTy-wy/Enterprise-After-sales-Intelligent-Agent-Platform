package com.agentplatform.backend.agent.application;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.List;

/**
 * Agent 问答请求。
 */
public record AgentChatRequest(
        String tenantId,
        String userId,
        String sessionId,
        String query,
        List<String> knowledgeBaseIds,
        String mode,
        @JsonIgnore
        String endUserAuthorization
) {
}
