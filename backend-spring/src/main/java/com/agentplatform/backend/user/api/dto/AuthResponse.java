package com.agentplatform.backend.user.api.dto;

/**
 * 登录成功响应。
 */
public record AuthResponse(
        String tokenType,
        String accessToken,
        long expiresInSeconds,
        UserResponse user
) {
}
