package com.agentplatform.backend.audit.infrastructure.repository;

import com.agentplatform.backend.audit.domain.AuditLog;
import com.agentplatform.backend.audit.domain.AuditLogRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 本地审计日志仓储。
 */
@Repository
@Profile("local")
public class InMemoryAuditLogRepository implements AuditLogRepository {

    private final CopyOnWriteArrayList<AuditLog> storage =
            new CopyOnWriteArrayList<>();

    @Override
    public AuditLog save(AuditLog auditLog) {
        storage.add(auditLog);
        return auditLog;
    }

    @Override
    public List<AuditLog> findByTenantId(String tenantId) {
        return storage.stream()
                .filter(log -> tenantId.equals(log.getTenantId()))
                .sorted((left, right) -> right.getCreatedAt()
                        .compareTo(left.getCreatedAt()))
                .toList();
    }
}
