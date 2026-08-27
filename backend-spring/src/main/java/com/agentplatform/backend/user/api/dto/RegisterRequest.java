package com.agentplatform.backend.user.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 注册请求。
 */
public record RegisterRequest(
        @NotBlank(message = "租户 ID 不能为空")
        @Size(max = 64, message = "租户 ID 不能超过 64 个字符")
        String tenantId,
        @NotBlank(message = "用户名不能为空")
        @Size(min = 3, max = 50, message = "用户名长度必须在 3 到 50 个字符之间")
        String username,
        @NotBlank(message = "显示名称不能为空")
        @Size(max = 100, message = "显示名称不能超过 100 个字符")
        String displayName,
        @NotBlank(message = "密码不能为空")
        @Size(min = 8, max = 100, message = "密码长度必须在 8 到 100 个字符之间")
        String password
) {
}
