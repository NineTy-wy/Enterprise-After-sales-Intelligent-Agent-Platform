package com.agentplatform.backend.user.domain;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

/**
 * 平台用户领域对象。
 */
public class UserAccount {

    private String id;
    private String tenantId;
    private String username;
    private String displayName;
    private String passwordHash;
    private Set<UserRole> roles;
    private UserStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public UserAccount() {
    }

    public static UserAccount register(
            String tenantId,
            String username,
            String displayName,
            String passwordHash,
            Set<UserRole> roles
    ) {
        LocalDateTime now = LocalDateTime.now();
        UserAccount account = new UserAccount();
        account.id = UUID.randomUUID().toString();
        account.tenantId = tenantId;
        account.username = username;
        account.displayName = displayName;
        account.passwordHash = passwordHash;
        account.roles = Set.copyOf(roles);
        account.status = UserStatus.ACTIVE;
        account.createdAt = now;
        account.updatedAt = now;
        return account;
    }

    public static UserAccount reconstitute(
            String id,
            String tenantId,
            String username,
            String displayName,
            String passwordHash,
            Set<UserRole> roles,
            UserStatus status,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        UserAccount account = new UserAccount();
        account.id = id;
        account.tenantId = tenantId;
        account.username = username;
        account.displayName = displayName;
        account.passwordHash = passwordHash;
        account.roles = Set.copyOf(roles);
        account.status = status;
        account.createdAt = createdAt;
        account.updatedAt = updatedAt;
        return account;
    }

    public boolean isActive() {
        return status == UserStatus.ACTIVE;
    }

    public String getId() {
        return id;
    }

    public String getTenantId() {
        return tenantId;
    }

    public String getUsername() {
        return username;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public Set<UserRole> getRoles() {
        return roles;
    }

    public UserStatus getStatus() {
        return status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
