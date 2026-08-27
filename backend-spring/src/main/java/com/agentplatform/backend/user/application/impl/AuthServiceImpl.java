package com.agentplatform.backend.user.application.impl;

import com.agentplatform.backend.common.error.BusinessException;
import com.agentplatform.backend.common.error.ErrorCode;
import com.agentplatform.backend.common.security.CurrentUser;
import com.agentplatform.backend.security.JwtTokenService;
import com.agentplatform.backend.user.api.dto.AuthResponse;
import com.agentplatform.backend.user.api.dto.LoginRequest;
import com.agentplatform.backend.user.api.dto.RegisterRequest;
import com.agentplatform.backend.user.api.dto.UserResponse;
import com.agentplatform.backend.user.application.AuthService;
import com.agentplatform.backend.user.domain.UserAccount;
import com.agentplatform.backend.user.domain.UserAccountRepository;
import com.agentplatform.backend.user.domain.UserRole;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Set;

/**
 * 认证应用服务实现。
 */
@Service
public class AuthServiceImpl implements AuthService {

    private final UserAccountRepository userAccountRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService jwtTokenService;

    public AuthServiceImpl(
            UserAccountRepository userAccountRepository,
            PasswordEncoder passwordEncoder,
            JwtTokenService jwtTokenService
    ) {
        this.userAccountRepository = userAccountRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenService = jwtTokenService;
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        UserAccount account = userAccountRepository
                .findByTenantIdAndUsername(request.tenantId(), request.username())
                .filter(UserAccount::isActive)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.UNAUTHORIZED,
                        "用户名、租户或密码错误"
                ));

        if (!passwordEncoder.matches(request.password(), account.getPasswordHash())) {
            throw new BusinessException(
                    ErrorCode.UNAUTHORIZED,
                    "用户名、租户或密码错误"
            );
        }

        return buildAuthResponse(account);
    }

    @Override
    public AuthResponse register(RegisterRequest request) {
        if (userAccountRepository.existsByTenantIdAndUsername(
                request.tenantId(),
                request.username()
        )) {
            throw new BusinessException(
                    ErrorCode.INVALID_REQUEST,
                    "当前租户下用户名已存在"
            );
        }

        UserAccount account = UserAccount.register(
                request.tenantId(),
                request.username(),
                request.displayName(),
                passwordEncoder.encode(request.password()),
                Set.of(UserRole.OPERATOR)
        );

        return buildAuthResponse(userAccountRepository.save(account));
    }

    private AuthResponse buildAuthResponse(UserAccount account) {
        CurrentUser currentUser = new CurrentUser(
                account.getId(),
                account.getTenantId(),
                account.getUsername(),
                account.getDisplayName(),
                account.getRoles()
        );
        return new AuthResponse(
                "Bearer",
                jwtTokenService.createToken(currentUser),
                jwtTokenService.getExpirationSeconds(),
                UserResponse.from(account)
        );
    }
}
