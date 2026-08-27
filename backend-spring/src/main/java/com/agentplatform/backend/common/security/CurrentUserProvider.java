package com.agentplatform.backend.common.security;

import com.agentplatform.backend.user.domain.UserRole;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.EnumSet;
import java.util.Set;

/**
 * 当前用户提供器。
 *
 * <p>安全开关关闭时返回本地演示身份，使开发者在没有配置 JWT、数据库
 * 和登录环境时仍然可以联调业务；生产环境应打开安全开关。</p>
 */
@Component
public class CurrentUserProvider {

    private final boolean securityEnabled;
    private final CurrentUser fallbackUser;

    public CurrentUserProvider(
            @Value("${app.security.enabled:false}") boolean securityEnabled,
            @Value("${app.identity.demo-tenant-id:tenant_demo}") String demoTenantId,
            @Value("${app.identity.demo-user-id:user_demo}") String demoUserId,
            @Value("${app.identity.demo-username:demo}") String demoUsername,
            @Value("${app.identity.demo-display-name:演示用户}") String demoDisplayName
    ) {
        this.securityEnabled = securityEnabled;
        this.fallbackUser = new CurrentUser(
                demoUserId,
                demoTenantId,
                demoUsername,
                demoDisplayName,
                EnumSet.of(UserRole.ADMIN, UserRole.OPERATOR)
        );
    }

    public CurrentUser currentUser() {
        if (!securityEnabled) {
            return fallbackUser;
        }

        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null
                && authentication.isAuthenticated()
                && authentication.getPrincipal() instanceof CurrentUser currentUser) {
            return currentUser;
        }

        return fallbackUser;
    }

    public boolean isSecurityEnabled() {
        return securityEnabled;
    }
}
