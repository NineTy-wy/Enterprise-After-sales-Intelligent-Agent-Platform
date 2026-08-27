package com.agentplatform.backend.user.domain;

import java.util.Optional;

/**
 * 用户仓储接口。
 */
public interface UserAccountRepository {

    UserAccount save(UserAccount account);

    Optional<UserAccount> findByTenantIdAndUsername(
            String tenantId,
            String username
    );

    boolean existsByTenantIdAndUsername(String tenantId, String username);
}
