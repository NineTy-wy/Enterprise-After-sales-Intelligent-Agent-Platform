package com.agentplatform.backend.user.infrastructure.persistence;

import com.agentplatform.backend.user.domain.UserAccount;
import com.agentplatform.backend.user.domain.UserAccountRepository;
import com.agentplatform.backend.user.domain.UserRole;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * PostgreSQL 开发环境种子账号。
 *
 * <p>数据库模式首次启动时，前端仍需要一个可登录账号完成联调。
 * 该初始化器只在 postgres Profile 生效，并且只在账号不存在时写入，
 * 不会覆盖已经存在的用户密码或角色。</p>
 */
@Component
@Profile("postgres")
public class DemoUserInitializer implements CommandLineRunner {

    private final UserAccountRepository userAccountRepository;
    private final PasswordEncoder passwordEncoder;
    private final boolean enabled;
    private final String tenantId;
    private final String username;
    private final String password;
    private final String displayName;

    public DemoUserInitializer(
            UserAccountRepository userAccountRepository,
            PasswordEncoder passwordEncoder,
            @Value("${app.identity.demo-user-enabled:true}") boolean enabled,
            @Value("${app.identity.demo-tenant-id:tenant_demo}") String tenantId,
            @Value("${app.identity.demo-username:demo}") String username,
            @Value("${app.identity.demo-password:demo123456}") String password,
            @Value("${app.identity.demo-display-name:演示用户}") String displayName
    ) {
        this.userAccountRepository = userAccountRepository;
        this.passwordEncoder = passwordEncoder;
        this.enabled = enabled;
        this.tenantId = tenantId;
        this.username = username;
        this.password = password;
        this.displayName = displayName;
    }

    @Override
    public void run(String... args) {
        if (!enabled || userAccountRepository
                .existsByTenantIdAndUsername(tenantId, username)) {
            return;
        }

        userAccountRepository.save(UserAccount.register(
                tenantId,
                username,
                displayName,
                passwordEncoder.encode(password),
                Set.of(UserRole.ADMIN, UserRole.OPERATOR)
        ));
    }
}
