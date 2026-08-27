package com.agentplatform.backend.tool.application;

/**
 * 工具调用响应。
 */
public record ToolCallResponse(
        String toolName,
        Object result,
        String traceId
) {
}
