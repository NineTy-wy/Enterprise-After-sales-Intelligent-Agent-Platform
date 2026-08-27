package com.agentplatform.backend.audit.infrastructure.persistence.repository;

import com.agentplatform.backend.audit.infrastructure.persistence.entity.AuditLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Spring Data 审计表访问接口。
 */
public interface SpringDataAuditLogRepository
        extends JpaRepository<AuditLogEntity, String> {

    List<AuditLogEntity> findAllByTenantIdOrderByCreatedAtDesc(String tenantId);
}
