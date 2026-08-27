package com.agentplatform.backend.audit.domain;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 审计日志领域对象。
 */
public class AuditLog {

    private String id;
    private String tenantId;
    private String actorId;
    private String action;
    private String resourceType;
    private String resourceId;
    private String detailJson;
    private LocalDateTime createdAt;

    public AuditLog() {
    }

    public static AuditLog create(
            String tenantId,
            String actorId,
            String action,
            String resourceType,
            String resourceId,
            String detailJson
    ) {
        AuditLog log = new AuditLog();
        log.id = UUID.randomUUID().toString();
        log.tenantId = tenantId;
        log.actorId = actorId;
        log.action = action;
        log.resourceType = resourceType;
        log.resourceId = resourceId;
        log.detailJson = detailJson;
        log.createdAt = LocalDateTime.now();
        return log;
    }

    public static AuditLog reconstitute(
            String id,
            String tenantId,
            String actorId,
            String action,
            String resourceType,
            String resourceId,
            String detailJson,
            LocalDateTime createdAt
    ) {
        AuditLog log = new AuditLog();
        log.id = id;
        log.tenantId = tenantId;
        log.actorId = actorId;
        log.action = action;
        log.resourceType = resourceType;
        log.resourceId = resourceId;
        log.detailJson = detailJson;
        log.createdAt = createdAt;
        return log;
    }

    public String getId() {
        return id;
    }

    public String getTenantId() {
        return tenantId;
    }

    public String getActorId() {
        return actorId;
    }

    public String getAction() {
        return action;
    }

    public String getResourceType() {
        return resourceType;
    }

    public String getResourceId() {
        return resourceId;
    }

    public String getDetailJson() {
        return detailJson;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
