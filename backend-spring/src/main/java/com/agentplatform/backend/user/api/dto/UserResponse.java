package com.agentplatform.backend.user.api.dto;

import com.agentplatform.backend.common.security.CurrentUser;
import com.agentplatform.backend.user.domain.UserAccount;
import com.agentplatform.backend.user.domain.UserRole;

import java.util.Set;

/**
 * 用户对外响应。
 */
public record UserResponse(
        String id,
        String tenantId,
        String username,
        String displayName,
        Set<UserRole> roles
) {

    public static UserResponse from(UserAccount account) {
        return new UserResponse(
                account.getId(),
                account.getTenantId(),
                account.getUsername(),
                account.getDisplayName(),
                account.getRoles()
        );
    }

    public static UserResponse from(CurrentUser user) {
        return new UserResponse(
                user.userId(),
                user.tenantId(),
                user.username(),
                user.displayName(),
                user.roles()
        );
    }
}
