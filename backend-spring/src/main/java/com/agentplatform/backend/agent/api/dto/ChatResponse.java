package com.agentplatform.backend.agent.api.dto;

import com.agentplatform.backend.agent.application.AgentChatResponse;
import com.agentplatform.backend.agent.application.AgentCitation;

import java.util.List;
import java.util.Map;

/**
 * 对外 Agent 问答响应。
 */
public record ChatResponse(
        String answer,
        List<Citation> citations,
        List<String> trace,
        Map<String, Object> tokenUsage,
        boolean fallbackUsed
) {

    public static ChatResponse from(AgentChatResponse response) {
        return new ChatResponse(
                response.answer(),
                response.citations().stream().map(Citation::from).toList(),
                response.trace(),
                response.tokenUsage(),
                response.fallbackUsed()
        );
    }

    public record Citation(
            String documentId,
            String fileName,
            String chunkId,
            double score,
            String content
    ) {
        public static Citation from(AgentCitation citation) {
            return new Citation(
                    citation.documentId(),
                    citation.fileName(),
                    citation.chunkId(),
                    citation.score(),
                    citation.content()
            );
        }
    }
}
