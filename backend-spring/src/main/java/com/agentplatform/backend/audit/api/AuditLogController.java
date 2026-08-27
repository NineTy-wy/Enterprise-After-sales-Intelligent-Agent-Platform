package com.agentplatform.backend.audit.api;

import com.agentplatform.backend.audit.api.dto.AuditLogResponse;
import com.agentplatform.backend.audit.application.AuditService;
import com.agentplatform.backend.common.api.ApiResponse;
import com.agentplatform.backend.common.security.CurrentUserProvider;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 审计查询接口。
 */
@RestController
@RequestMapping("/api/audit-logs")
public class AuditLogController {

    private final AuditService auditService;
    private final CurrentUserProvider currentUserProvider;

    public AuditLogController(
            AuditService auditService,
            CurrentUserProvider currentUserProvider
    ) {
        this.auditService = auditService;
        this.currentUserProvider = currentUserProvider;
    }

    @GetMapping
    public ApiResponse<List<AuditLogResponse>> list() {
        return ApiResponse.success(auditService.list(
                        currentUserProvider.currentUser().tenantId()
                ).stream()
                .map(AuditLogResponse::from)
                .toList());
    }
}
