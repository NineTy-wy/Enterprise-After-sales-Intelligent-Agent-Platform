package com.agentplatform.backend.audit.infrastructure.persistence.repository;

import com.agentplatform.backend.audit.domain.AuditLog;
import com.agentplatform.backend.audit.domain.AuditLogRepository;
import com.agentplatform.backend.audit.infrastructure.persistence.entity.AuditLogEntity;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * PostgreSQL 审计日志仓储适配器。
 */
@Repository
@Profile("postgres")
public class PostgresAuditLogRepository implements AuditLogRepository {

    private final SpringDataAuditLogRepository repository;

    public PostgresAuditLogRepository(SpringDataAuditLogRepository repository) {
        this.repository = repository;
    }

    @Override
    public AuditLog save(AuditLog auditLog) {
        return repository.save(AuditLogEntity.from(auditLog)).toDomain();
    }

    @Override
    public List<AuditLog> findByTenantId(String tenantId) {
        return repository.findAllByTenantIdOrderByCreatedAtDesc(tenantId)
                .stream()
                .map(AuditLogEntity::toDomain)
                .toList();
    }
}
