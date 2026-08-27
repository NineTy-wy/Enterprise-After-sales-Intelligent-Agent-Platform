package com.agentplatform.backend.user.infrastructure.persistence.entity;

import com.agentplatform.backend.user.domain.UserAccount;
import com.agentplatform.backend.user.domain.UserRole;
import com.agentplatform.backend.user.domain.UserStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 用户数据库实体。
 */
@Entity
@Table(
        name = "user_accounts",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_user_tenant_username",
                columnNames = {"tenant_id", "username"}
        )
)
public class UserAccountEntity {

    @Id
    @Column(length = 36, nullable = false, updatable = false)
    private String id;

    @Column(name = "tenant_id", length = 64, nullable = false)
    private String tenantId;

    @Column(length = 100, nullable = false)
    private String username;

    @Column(name = "display_name", length = 100, nullable = false)
    private String displayName;

    @Column(name = "password_hash", length = 255, nullable = false)
    private String passwordHash;

    @Column(length = 500, nullable = false)
    private String roles;

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private UserStatus status;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected UserAccountEntity() {
        // JPA 需要无参构造方法。
    }

    private UserAccountEntity(UserAccount account) {
        this.id = account.getId();
        this.tenantId = account.getTenantId();
        this.username = account.getUsername();
        this.displayName = account.getDisplayName();
        this.passwordHash = account.getPasswordHash();
        this.roles = account.getRoles().stream().map(Enum::name)
                .sorted()
                .collect(Collectors.joining(","));
        this.status = account.getStatus();
        this.createdAt = account.getCreatedAt();
        this.updatedAt = account.getUpdatedAt();
    }

    public static UserAccountEntity from(UserAccount account) {
        return new UserAccountEntity(account);
    }

    public UserAccount toDomain() {
        Set<UserRole> roleSet = Arrays.stream(roles.split(","))
                .filter(value -> !value.isBlank())
                .map(UserRole::valueOf)
                .collect(Collectors.toUnmodifiableSet());
        return UserAccount.reconstitute(
                id,
                tenantId,
                username,
                displayName,
                passwordHash,
                roleSet,
                status,
                createdAt,
                updatedAt
        );
    }
}
