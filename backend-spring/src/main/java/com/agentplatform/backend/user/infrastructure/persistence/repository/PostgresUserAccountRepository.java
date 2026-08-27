package com.agentplatform.backend.user.infrastructure.persistence.repository;

import com.agentplatform.backend.user.domain.UserAccount;
import com.agentplatform.backend.user.domain.UserAccountRepository;
import com.agentplatform.backend.user.infrastructure.persistence.entity.UserAccountEntity;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * PostgreSQL 用户仓储适配器。
 */
@Repository
@Profile("postgres")
public class PostgresUserAccountRepository implements UserAccountRepository {

    private final SpringDataUserAccountRepository repository;

    public PostgresUserAccountRepository(
            SpringDataUserAccountRepository repository
    ) {
        this.repository = repository;
    }

    @Override
    public UserAccount save(UserAccount account) {
        return repository.save(UserAccountEntity.from(account)).toDomain();
    }

    @Override
    public Optional<UserAccount> findByTenantIdAndUsername(
            String tenantId,
            String username
    ) {
        return repository.findByTenantIdAndUsername(tenantId, username)
                .map(UserAccountEntity::toDomain);
    }

    @Override
    public boolean existsByTenantIdAndUsername(
            String tenantId,
            String username
    ) {
        return repository.existsByTenantIdAndUsername(tenantId, username);
    }
}
