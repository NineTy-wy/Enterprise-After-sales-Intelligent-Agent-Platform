package com.agentplatform.backend.user.api;

import com.agentplatform.backend.common.api.ApiResponse;
import com.agentplatform.backend.common.security.CurrentUserProvider;
import com.agentplatform.backend.user.api.dto.AuthResponse;
import com.agentplatform.backend.user.api.dto.LoginRequest;
import com.agentplatform.backend.user.api.dto.RegisterRequest;
import com.agentplatform.backend.user.application.AuthService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 登录和注册接口。
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final CurrentUserProvider currentUserProvider;

    public AuthController(
            AuthService authService,
            CurrentUserProvider currentUserProvider
    ) {
        this.authService = authService;
        this.currentUserProvider = currentUserProvider;
    }

    @PostMapping("/login")
    public ApiResponse<AuthResponse> login(
            @Valid @RequestBody LoginRequest request
    ) {
        return ApiResponse.success(authService.login(request));
    }

    @PostMapping("/register")
    public ApiResponse<AuthResponse> register(
            @Valid @RequestBody RegisterRequest request
    ) {
        return ApiResponse.success(authService.register(request));
    }

    @org.springframework.web.bind.annotation.GetMapping("/me")
    public ApiResponse<com.agentplatform.backend.user.api.dto.UserResponse> me() {
        return ApiResponse.success(
                com.agentplatform.backend.user.api.dto.UserResponse.from(
                        currentUserProvider.currentUser()
                )
        );
    }
}
