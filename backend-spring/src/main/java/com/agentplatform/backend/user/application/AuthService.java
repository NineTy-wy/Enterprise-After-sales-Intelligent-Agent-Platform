package com.agentplatform.backend.user.application;

import com.agentplatform.backend.user.api.dto.AuthResponse;
import com.agentplatform.backend.user.api.dto.LoginRequest;
import com.agentplatform.backend.user.api.dto.RegisterRequest;

/**
 * 认证应用服务。
 */
public interface AuthService {

    AuthResponse login(LoginRequest request);

    AuthResponse register(RegisterRequest request);
}
