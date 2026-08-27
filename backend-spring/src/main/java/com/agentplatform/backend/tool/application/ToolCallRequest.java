package com.agentplatform.backend.tool.application;

import java.util.Map;

/**
 * 工具调用请求。
 */
public record ToolCallRequest(Map<String, Object> arguments) {
}
