package com.agentplatform.backend.agent.infrastructure;

import com.agentplatform.backend.agent.application.AgentChatRequest;
import com.agentplatform.backend.agent.application.AgentChatResponse;
import com.agentplatform.backend.agent.application.AgentCitation;
import com.agentplatform.backend.agent.application.AgentIngestRequest;
import com.agentplatform.backend.agent.application.AgentIngestResult;
import com.agentplatform.backend.agent.application.AgentServiceClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 本地 Agent 降级实现。
 *
 * <p>它不是最终模型实现，而是让前后端在未配置模型 API 时仍能完整联调。
 * 切换 app.agent.mode=http 后会使用真实 FastAPI 服务。</p>
 */
@Component
@ConditionalOnProperty(
        name = "app.agent.mode",
        havingValue = "mock",
        matchIfMissing = true
)
public class MockAgentServiceClient implements AgentServiceClient {

    @Override
    public AgentIngestResult ingest(AgentIngestRequest request) {
        return new AgentIngestResult(
                "INDEXED",
                1,
                "本地 Mock Agent 已完成解析和向量化模拟"
        );
    }

    @Override
    public AgentChatResponse chat(AgentChatRequest request) {
        String query = request.query() == null ? "" : request.query().trim();
        String answer = query.isBlank()
                ? "请描述需要处理的售后问题。"
                : "这是本地 Agent 降级回答。已收到问题：“" + query
                + "”。配置 FastAPI、Embedding 和大模型后，将返回基于知识库引用的正式答案。";

        return new AgentChatResponse(
                answer,
                List.of(),
                List.of(
                        "query_rewrite: skipped_in_mock_mode",
                        "hybrid_retrieval: fallback",
                        "rerank: fallback",
                        "llm_generation: mock"
                ),
                Map.of("promptTokens", 0, "completionTokens", 0, "totalTokens", 0),
                true
        );
    }
}
