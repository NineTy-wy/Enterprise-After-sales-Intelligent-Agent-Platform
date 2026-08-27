package com.agentplatform.backend.user.infrastructure.persistence.repository;

import com.agentplatform.backend.user.infrastructure.persistence.entity.UserAccountEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Spring Data 用户表访问接口。
 */
public interface SpringDataUserAccountRepository
        extends JpaRepository<UserAccountEntity, String> {

    Optional<UserAccountEntity> findByTenantIdAndUsername(
            String tenantId,
            String username
    );

    boolean existsByTenantIdAndUsername(String tenantId, String username);
}
