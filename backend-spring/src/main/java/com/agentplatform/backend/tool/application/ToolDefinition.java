package com.agentplatform.backend.tool.application;

import com.agentplatform.backend.user.domain.UserRole;

import java.util.Map;

/**
 * 可调用工具定义。
 */
public record ToolDefinition(
        String name,
        String description,
        UserRole requiredRole,
        Map<String, Object> inputSchema
) {
}
