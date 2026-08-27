package com.agentplatform.backend.agent.application;

import com.agentplatform.backend.agent.api.dto.ChatRequest;
import com.agentplatform.backend.agent.api.dto.ChatResponse;
import com.agentplatform.backend.common.security.CurrentUser;

/**
 * Agent 网关应用服务。
 */
public interface AgentGatewayService {

    ChatResponse chat(
            CurrentUser user,
            ChatRequest request,
            String endUserAuthorization
    );
}
