package com.agentplatform.backend.audit.infrastructure.persistence.entity;

import com.agentplatform.backend.audit.domain.AuditLog;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * 审计日志数据库实体。
 */
@Entity
@Table(name = "audit_logs")
public class AuditLogEntity {

    @Id
    @Column(length = 36, nullable = false, updatable = false)
    private String id;

    @Column(name = "tenant_id", length = 64, nullable = false)
    private String tenantId;

    @Column(name = "actor_id", length = 64, nullable = false)
    private String actorId;

    @Column(length = 100, nullable = false)
    private String action;

    @Column(name = "resource_type", length = 50, nullable = false)
    private String resourceType;

    @Column(name = "resource_id", length = 100)
    private String resourceId;

    @Column(name = "detail_json", columnDefinition = "text")
    private String detailJson;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    protected AuditLogEntity() {
    }

    private AuditLogEntity(AuditLog log) {
        id = log.getId();
        tenantId = log.getTenantId();
        actorId = log.getActorId();
        action = log.getAction();
        resourceType = log.getResourceType();
        resourceId = log.getResourceId();
        detailJson = log.getDetailJson();
        createdAt = log.getCreatedAt();
    }

    public static AuditLogEntity from(AuditLog log) {
        return new AuditLogEntity(log);
    }

    public AuditLog toDomain() {
        return AuditLog.reconstitute(
                id,
                tenantId,
                actorId,
                action,
                resourceType,
                resourceId,
                detailJson,
                createdAt
        );
    }
}
