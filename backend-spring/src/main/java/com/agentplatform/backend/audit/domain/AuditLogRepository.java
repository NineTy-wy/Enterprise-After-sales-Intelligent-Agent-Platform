package com.agentplatform.backend.audit.domain;

import java.util.List;

/**
 * 审计日志仓储接口。
 */
public interface AuditLogRepository {

    AuditLog save(AuditLog auditLog);

    List<AuditLog> findByTenantId(String tenantId);
}
