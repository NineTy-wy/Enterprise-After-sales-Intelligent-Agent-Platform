package com.agentplatform.backend.common.security;

import com.agentplatform.backend.user.domain.UserRole;

import java.util.Set;

/**
 * 当前请求的身份信息。
 *
 * <p>业务层只接收这个稳定的身份对象，不直接依赖 Spring Security 的具体实现，
 * 便于单元测试和后续接入网关、服务账号或 SSO。</p>
 */
public record CurrentUser(
        String userId,
        String tenantId,
        String username,
        String displayName,
        Set<UserRole> roles
) {

    public boolean hasRole(UserRole role) {
        return roles != null && roles.contains(role);
    }
}
