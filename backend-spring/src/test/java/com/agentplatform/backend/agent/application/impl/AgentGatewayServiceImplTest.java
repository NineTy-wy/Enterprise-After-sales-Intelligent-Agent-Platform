package com.agentplatform.backend.agent.application.impl;

import com.agentplatform.backend.agent.api.dto.ChatRequest;
import com.agentplatform.backend.agent.api.dto.ChatResponse;
import com.agentplatform.backend.agent.application.AgentChatRequest;
import com.agentplatform.backend.agent.application.AgentChatResponse;
import com.agentplatform.backend.agent.application.AgentCitation;
import com.agentplatform.backend.agent.application.AgentIngestRequest;
import com.agentplatform.backend.agent.application.AgentIngestResult;
import com.agentplatform.backend.agent.application.AgentResponseCache;
import com.agentplatform.backend.agent.application.AgentServiceClient;
import com.agentplatform.backend.agent.application.SensitiveContentFilter;
import com.agentplatform.backend.common.error.BusinessException;
import com.agentplatform.backend.common.error.ErrorCode;
import com.agentplatform.backend.common.security.CurrentUser;
import com.agentplatform.backend.knowledge.api.dto.CreateKnowledgeBaseRequest;
import com.agentplatform.backend.knowledge.api.dto.KnowledgeBaseResponse;
import com.agentplatform.backend.knowledge.application.KnowledgeBaseService;
import com.agentplatform.backend.knowledge.application.impl.KnowledgeBaseServiceImpl;
import com.agentplatform.backend.knowledge.infrastructure.repository.InMemoryKnowledgeBaseRepository;
import com.agentplatform.backend.user.domain.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Agent 网关服务单元测试。
 *
 * <p>这里不调用真实 FastAPI，而是用 Fake Client 验证 Spring 业务网关必须
 * 承担的安全边界：租户知识库校验、归档资源拦截、敏感信息脱敏和缓存。</p>
 */
class AgentGatewayServiceImplTest {

    private static final String TENANT_ID = "tenant_test";
    private static final String USER_ID = "user_test";

    private KnowledgeBaseService knowledgeBaseService;
    private FakeAgentServiceClient agentServiceClient;
    private AgentGatewayServiceImpl agentGatewayService;
    private CurrentUser currentUser;

    @BeforeEach
    void setUp() {
        knowledgeBaseService = new KnowledgeBaseServiceImpl(
                new InMemoryKnowledgeBaseRepository()
        );
        agentServiceClient = new FakeAgentServiceClient();
        agentGatewayService = new AgentGatewayServiceImpl(
                agentServiceClient,
                new SensitiveContentFilter("密码,secret,api_key"),
                new AgentResponseCache(true, 60),
                knowledgeBaseService
        );
        currentUser = new CurrentUser(
                USER_ID,
                TENANT_ID,
                "operator",
                "测试用户",
                Set.of(UserRole.OPERATOR)
        );
    }

    @Test
    void chat_shouldFilterSensitiveContentAndCacheSameQuestion() {
        KnowledgeBaseResponse knowledgeBase = createKnowledgeBase("售后知识库");

        ChatResponse firstResponse = agentGatewayService.chat(
                currentUser,
                new ChatRequest(
                        "session-1",
                        "客户手机号13812345678，设备出现E03报警，密码是123456",
                        List.of(knowledgeBase.id()),
                        "react"
                ),
                "Bearer user-token"
        );

        ChatResponse secondResponse = agentGatewayService.chat(
                currentUser,
                new ChatRequest(
                        "session-1",
                        "客户手机号13812345678，设备出现E03报警，密码是123456",
                        List.of(knowledgeBase.id()),
                        "react"
                ),
                "Bearer user-token"
        );

        assertEquals(1, agentServiceClient.chatCallCount);
        assertEquals(firstResponse.answer(), secondResponse.answer());
        assertFalse(agentServiceClient.lastRequest.query().contains("13812345678"));
        assertFalse(firstResponse.answer().contains("13812345678"));
        assertTrue(firstResponse.answer().contains("[手机号已脱敏]"));
    }

    @Test
    void chat_shouldRejectArchivedKnowledgeBaseBeforeCallingAgent() {
        KnowledgeBaseResponse knowledgeBase = createKnowledgeBase("归档知识库");
        knowledgeBaseService.archiveKnowledgeBase(
                TENANT_ID,
                USER_ID,
                knowledgeBase.id()
        );

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> agentGatewayService.chat(
                        currentUser,
                        new ChatRequest(
                                null,
                                "E03报警怎么处理",
                                List.of(knowledgeBase.id()),
                                "plan_execute"
                        ),
                        null
                )
        );

        assertEquals(ErrorCode.RESOURCE_STATE_CONFLICT, exception.getErrorCode());
        assertEquals(0, agentServiceClient.chatCallCount);
    }

    private KnowledgeBaseResponse createKnowledgeBase(String name) {
        return knowledgeBaseService.createKnowledgeBase(
                TENANT_ID,
                USER_ID,
                new CreateKnowledgeBaseRequest(name, "测试知识库")
        );
    }

    private static class FakeAgentServiceClient implements AgentServiceClient {

        private int chatCallCount;
        private AgentChatRequest lastRequest;

        @Override
        public AgentIngestResult ingest(AgentIngestRequest request) {
            return new AgentIngestResult(
                    "INDEXED",
                    1,
                    "fake indexed"
            );
        }

        @Override
        public AgentChatResponse chat(AgentChatRequest request) {
            chatCallCount++;
            lastRequest = request;
            return new AgentChatResponse(
                    "已收到问题：" + request.query(),
                    List.of(new AgentCitation(
                            "document-1",
                            "manual.pdf",
                            "chunk-1",
                            0.98,
                            "客户手机号13812345678，需要先检查传感器。"
                    )),
                    List.of("RetrievalAgent: fake"),
                    Map.of(
                            "promptTokens", 8,
                            "completionTokens", 4,
                            "totalTokens", 12
                    ),
                    false
            );
        }
    }
}
