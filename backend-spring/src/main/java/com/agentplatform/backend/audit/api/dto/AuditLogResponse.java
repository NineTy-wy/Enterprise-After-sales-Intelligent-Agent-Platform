package com.agentplatform.backend.audit.api.dto;

import com.agentplatform.backend.audit.domain.AuditLog;

import java.time.LocalDateTime;

/**
 * 审计日志响应。
 */
public record AuditLogResponse(
        String id,
        String actorId,
        String action,
        String resourceType,
        String resourceId,
        String detailJson,
        LocalDateTime createdAt
) {

    public static AuditLogResponse from(AuditLog log) {
        return new AuditLogResponse(
                log.getId(),
                log.getActorId(),
                log.getAction(),
                log.getResourceType(),
                log.getResourceId(),
                log.getDetailJson(),
                log.getCreatedAt()
        );
    }
}
