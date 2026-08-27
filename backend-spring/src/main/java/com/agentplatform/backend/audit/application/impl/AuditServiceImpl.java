package com.agentplatform.backend.audit.application.impl;

import com.agentplatform.backend.audit.application.AuditService;
import com.agentplatform.backend.audit.domain.AuditLog;
import com.agentplatform.backend.audit.domain.AuditLogRepository;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 审计应用服务实现。
 */
@Service
public class AuditServiceImpl implements AuditService {

    private final AuditLogRepository auditLogRepository;

    public AuditServiceImpl(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Override
    public void record(
            String tenantId,
            String actorId,
            String action,
            String resourceType,
            String resourceId,
            String detailJson
    ) {
        auditLogRepository.save(AuditLog.create(
                tenantId,
                actorId,
                action,
                resourceType,
                resourceId,
                detailJson
        ));
    }

    @Override
    public List<AuditLog> list(String tenantId) {
        return auditLogRepository.findByTenantId(tenantId);
    }
}
