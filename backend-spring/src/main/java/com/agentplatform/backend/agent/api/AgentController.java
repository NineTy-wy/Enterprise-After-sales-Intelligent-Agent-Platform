package com.agentplatform.backend.agent.api;

import com.agentplatform.backend.audit.application.AuditService;
import com.agentplatform.backend.agent.api.dto.ChatRequest;
import com.agentplatform.backend.agent.api.dto.ChatResponse;
import com.agentplatform.backend.agent.application.AgentGatewayService;
import com.agentplatform.backend.common.api.ApiResponse;
import com.agentplatform.backend.common.security.CurrentUser;
import com.agentplatform.backend.common.security.CurrentUserProvider;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;

/**
 * Agent 对外网关。
 */
@RestController
@RequestMapping("/api/agent")
public class AgentController {

    private final AgentGatewayService agentGatewayService;
    private final CurrentUserProvider currentUserProvider;
    private final AuditService auditService;

    public AgentController(
            AgentGatewayService agentGatewayService,
            CurrentUserProvider currentUserProvider,
            AuditService auditService
    ) {
        this.agentGatewayService = agentGatewayService;
        this.currentUserProvider = currentUserProvider;
        this.auditService = auditService;
    }

    @PostMapping("/chat")
    public ApiResponse<ChatResponse> chat(
            @Valid @RequestBody ChatRequest request,
            @RequestHeader(
                    value = "Authorization",
                    required = false
            ) String endUserAuthorization
    ) {
        CurrentUser user = currentUserProvider.currentUser();
        ChatResponse response = agentGatewayService.chat(
                user,
                request,
                endUserAuthorization
        );
        auditService.record(
                user.tenantId(),
                user.userId(),
                "CHAT",
                "AGENT",
                request.sessionId(),
                "{\"mode\":\"" + (request.mode() == null ? "react" : request.mode())
                        + "\",\"fallbackUsed\":" + response.fallbackUsed() + "}"
        );
        return ApiResponse.success(response);
    }

    /**
     * 对前端提供 SSE。底层 Agent 可以逐步升级为真实流式模型，
     * 当前实现先将完整回答切成小片段，保持前端协议稳定。
     */
    @PostMapping(
            value = "/chat/stream",
            produces = MediaType.TEXT_EVENT_STREAM_VALUE
    )
    public SseEmitter chatStream(
            @Valid @RequestBody ChatRequest request,
            @RequestHeader(
                    value = "Authorization",
                    required = false
            ) String endUserAuthorization
    ) {
        SseEmitter emitter = new SseEmitter(60_000L);
        CurrentUser user = currentUserProvider.currentUser();
        CompletableFuture.runAsync(() -> {
            try {
                ChatResponse response = agentGatewayService.chat(
                        user,
                        request,
                        endUserAuthorization
                );
                Arrays.stream(response.answer().split("(?<=。|！|？|\\.|!|\\?)"))
                        .filter(item -> !item.isBlank())
                        .forEach(item -> send(emitter, item));
                emitter.send(SseEmitter.event()
                        .name("done")
                        .data(response));
                emitter.complete();
            } catch (Exception exception) {
                emitter.completeWithError(exception);
            }
        });
        return emitter;
    }

    private void send(SseEmitter emitter, String content) {
        try {
            emitter.send(SseEmitter.event().name("message").data(content));
        } catch (IOException exception) {
            throw new IllegalStateException("SSE 推送失败", exception);
        }
    }
}
