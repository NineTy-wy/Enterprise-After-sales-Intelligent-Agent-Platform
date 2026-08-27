package com.agentplatform.backend.agent.application;

import java.util.List;
import java.util.Map;

/**
 * Agent 问答结果。
 */
public record AgentChatResponse(
        String answer,
        List<AgentCitation> citations,
        List<String> trace,
        Map<String, Object> tokenUsage,
        boolean fallbackUsed
) {
}
