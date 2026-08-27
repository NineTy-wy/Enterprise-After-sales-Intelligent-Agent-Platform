package com.agentplatform.backend.tool.application;

import com.agentplatform.backend.common.security.CurrentUser;

import java.util.List;

/**
 * 工具注册和调用服务。
 */
public interface ToolService {

    List<ToolDefinition> listTools(CurrentUser user);

    ToolCallResponse invoke(
            CurrentUser user,
            String toolName,
            ToolCallRequest request
    );
}
