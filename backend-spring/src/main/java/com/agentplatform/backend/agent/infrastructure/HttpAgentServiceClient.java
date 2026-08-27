package com.agentplatform.backend.agent.infrastructure;

import com.agentplatform.backend.agent.application.AgentChatRequest;
import com.agentplatform.backend.agent.application.AgentChatResponse;
import com.agentplatform.backend.agent.application.AgentIngestRequest;
import com.agentplatform.backend.agent.application.AgentIngestResult;
import com.agentplatform.backend.agent.application.AgentServiceClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * HTTP Agent 服务客户端。
 */
@Component
@ConditionalOnProperty(
        name = "app.agent.mode",
        havingValue = "http"
)
public class HttpAgentServiceClient implements AgentServiceClient {

    private final RestClient restClient;
    private final String serviceToken;

    public HttpAgentServiceClient(
            RestClient.Builder restClientBuilder,
            @Value("${app.agent.base-url}") String baseUrl,
            @Value("${app.agent.service-token:}") String serviceToken
    ) {
        this.restClient = restClientBuilder.baseUrl(baseUrl).build();
        this.serviceToken = serviceToken;
    }

    @Override
    public AgentIngestResult ingest(AgentIngestRequest request) {
        try {
            return restClient.post()
                    .uri("/v1/ingest")
                    .contentType(MediaType.APPLICATION_JSON)
                    .headers(headers -> addHeaders(headers, null))
                    .body(request)
                    .retrieve()
                    .body(AgentIngestResult.class);
        } catch (RuntimeException exception) {
            throw new IllegalStateException("Agent 服务不可用", exception);
        }
    }

    @Override
    public AgentChatResponse chat(AgentChatRequest request) {
        try {
            return restClient.post()
                    .uri("/v1/chat")
                    .contentType(MediaType.APPLICATION_JSON)
                    .headers(headers -> addHeaders(
                            headers,
                            request.endUserAuthorization()
                    ))
                    .body(request)
                    .retrieve()
                    .body(AgentChatResponse.class);
        } catch (RuntimeException exception) {
            // 模型服务短暂不可用时给出可解释的降级结果，避免整个售后工作台 500。
            return new AgentChatResponse(
                    "当前智能服务暂时不可用，请先按人工流程处理，并稍后重试。",
                    java.util.List.of(),
                    java.util.List.of("model_fallback: agent_service_unavailable"),
                    java.util.Map.of(
                            "promptTokens", 0,
                            "completionTokens", 0,
                            "totalTokens", 0
                    ),
                    true
            );
        }
    }

    private void addHeaders(
            org.springframework.http.HttpHeaders headers,
            String endUserAuthorization
    ) {
        if (serviceToken != null && !serviceToken.isBlank()) {
            headers.setBearerAuth(serviceToken);
        }
        if (endUserAuthorization != null && !endUserAuthorization.isBlank()) {
            headers.set("X-End-User-Authorization", endUserAuthorization);
        }
    }
}
