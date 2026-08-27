package com.agentplatform.backend.user.infrastructure.repository;

import com.agentplatform.backend.user.domain.UserAccount;
import com.agentplatform.backend.user.domain.UserAccountRepository;
import com.agentplatform.backend.user.domain.UserRole;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 本地开发用户仓储。
 *
 * <p>启动时预置 demo/demo123456，方便在没有数据库时联调登录页面。
 * 生产环境切换到 postgres Profile 后使用数据库实现。</p>
 */
@Repository
@Profile("local")
public class InMemoryUserAccountRepository implements UserAccountRepository {

    private final ConcurrentMap<String, UserAccount> storage =
            new ConcurrentHashMap<>();

    public InMemoryUserAccountRepository(
            PasswordEncoder passwordEncoder,
            @Value("${app.identity.demo-tenant-id:tenant_demo}") String tenantId,
            @Value("${app.identity.demo-username:demo}") String username,
            @Value("${app.identity.demo-password:demo123456}") String password,
            @Value("${app.identity.demo-display-name:演示用户}") String displayName,
            @Value("${app.identity.demo-user-enabled:true}") boolean enabled
    ) {
        if (!enabled) {
            return;
        }
        UserAccount demo = UserAccount.register(
                tenantId,
                username,
                displayName,
                passwordEncoder.encode(password),
                java.util.Set.of(UserRole.ADMIN, UserRole.OPERATOR)
        );
        storage.put(key(demo.getTenantId(), demo.getUsername()), demo);
    }

    @Override
    public UserAccount save(UserAccount account) {
        storage.put(key(account.getTenantId(), account.getUsername()), account);
        return account;
    }

    @Override
    public Optional<UserAccount> findByTenantIdAndUsername(
            String tenantId,
            String username
    ) {
        return Optional.ofNullable(storage.get(key(tenantId, username)));
    }

    @Override
    public boolean existsByTenantIdAndUsername(
            String tenantId,
            String username
    ) {
        return storage.containsKey(key(tenantId, username));
    }

    private String key(String tenantId, String username) {
        return tenantId + ":" + username;
    }
}
