package com.agentplatform.backend.audit.application;

import com.agentplatform.backend.audit.domain.AuditLog;

import java.util.List;

/**
 * 审计应用服务。
 */
public interface AuditService {

    void record(
            String tenantId,
            String actorId,
            String action,
            String resourceType,
            String resourceId,
            String detailJson
    );

    List<AuditLog> list(String tenantId);
}
