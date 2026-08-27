package com.agentplatform.backend.agent.application.impl;

import com.agentplatform.backend.agent.api.dto.ChatRequest;
import com.agentplatform.backend.agent.api.dto.ChatResponse;
import com.agentplatform.backend.agent.application.AgentChatRequest;
import com.agentplatform.backend.agent.application.AgentChatResponse;
import com.agentplatform.backend.agent.application.AgentGatewayService;
import com.agentplatform.backend.agent.application.AgentResponseCache;
import com.agentplatform.backend.agent.application.AgentServiceClient;
import com.agentplatform.backend.agent.application.SensitiveContentFilter;
import com.agentplatform.backend.common.security.CurrentUser;
import com.agentplatform.backend.common.error.BusinessException;
import com.agentplatform.backend.common.error.ErrorCode;
import com.agentplatform.backend.knowledge.api.dto.KnowledgeBaseResponse;
import com.agentplatform.backend.knowledge.application.KnowledgeBaseService;
import com.agentplatform.backend.knowledge.domain.KnowledgeBaseStatus;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
import java.util.HexFormat;

/**
 * Agent 网关应用服务实现。
 */
@Service
public class AgentGatewayServiceImpl implements AgentGatewayService {

    private final AgentServiceClient agentServiceClient;
    private final SensitiveContentFilter sensitiveContentFilter;
    private final AgentResponseCache agentResponseCache;
    private final KnowledgeBaseService knowledgeBaseService;

    public AgentGatewayServiceImpl(
            AgentServiceClient agentServiceClient,
            SensitiveContentFilter sensitiveContentFilter,
            AgentResponseCache agentResponseCache,
            KnowledgeBaseService knowledgeBaseService
    ) {
        this.agentServiceClient = agentServiceClient;
        this.sensitiveContentFilter = sensitiveContentFilter;
        this.agentResponseCache = agentResponseCache;
        this.knowledgeBaseService = knowledgeBaseService;
    }

    @Override
    public ChatResponse chat(
            CurrentUser user,
            ChatRequest request,
            String endUserAuthorization
    ) {
        List<String> knowledgeBaseIds = request.knowledgeBaseIds() == null
                ? List.of()
                : request.knowledgeBaseIds().stream()
                .filter(item -> item != null && !item.isBlank())
                .distinct()
                .toList();
        validateKnowledgeBases(user.tenantId(), knowledgeBaseIds);
        String filteredQuery = sensitiveContentFilter.filter(request.query());
        String mode = request.mode() == null || request.mode().isBlank()
                ? "react"
                : request.mode();
        String cacheKey = cacheKey(
                user,
                filteredQuery,
                knowledgeBaseIds,
                mode,
                request.sessionId()
        );
        ChatResponse cached = agentResponseCache.get(cacheKey);
        if (cached != null) {
            return cached;
        }
        AgentChatResponse response = agentServiceClient.chat(new AgentChatRequest(
                user.tenantId(),
                user.userId(),
                request.sessionId(),
                filteredQuery,
                knowledgeBaseIds,
                mode,
                endUserAuthorization
        ));

        if (response == null) {
            response = new AgentChatResponse(
                    "当前智能服务未返回有效结果，请稍后重试或转人工处理。",
                    List.of(),
                    List.of("model_fallback: empty_agent_response"),
                    java.util.Map.of(
                            "promptTokens", 0,
                            "completionTokens", 0,
                            "totalTokens", 0
                    ),
                    true
            );
        }

        ChatResponse result = ChatResponse.from(new AgentChatResponse(
                sensitiveContentFilter.filter(response.answer()),
                response.citations().stream()
                        .map(citation -> new com.agentplatform.backend.agent.application.AgentCitation(
                                citation.documentId(),
                                citation.fileName(),
                                citation.chunkId(),
                                citation.score(),
                                sensitiveContentFilter.filter(citation.content())
                        ))
                        .toList(),
                response.trace(),
                response.tokenUsage(),
                response.fallbackUsed()
        ));
        agentResponseCache.put(cacheKey, result);
        return result;
    }

    /**
     * 在进入 Agent 服务前再次校验知识库归属和状态。
     *
     * <p>不能只依赖前端传入的 ID；归档知识库也不应继续参与在线问答。
     * 该校验同时避免把资源权限边界交给外部 Agent 服务。</p>
     */
    private void validateKnowledgeBases(
            String tenantId,
            List<String> knowledgeBaseIds
    ) {
        for (String knowledgeBaseId : knowledgeBaseIds) {
            KnowledgeBaseResponse knowledgeBase =
                    knowledgeBaseService.getKnowledgeBase(tenantId, knowledgeBaseId);
            if (knowledgeBase.status() != KnowledgeBaseStatus.ACTIVE) {
                throw new BusinessException(
                        ErrorCode.RESOURCE_STATE_CONFLICT,
                        "已归档知识库不能参与在线问答"
                );
            }
        }
    }

    private String cacheKey(
            CurrentUser user,
            String query,
            List<String> knowledgeBaseIds,
            String mode,
            String sessionId
    ) {
        String raw = user.tenantId() + "|" + user.userId() + "|" + query + "|"
                + String.join(",", knowledgeBaseIds) + "|" + mode + "|"
                + (sessionId == null ? "" : sessionId);
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(raw.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            return raw;
        }
    }
}
