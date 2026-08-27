package com.agentplatform.backend.knowledge.infrastructure.persistence.entity;

import com.agentplatform.backend.knowledge.domain.KnowledgeBase;
import com.agentplatform.backend.knowledge.domain.KnowledgeBaseStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.LocalDateTime;

/**
 * 知识库数据库实体。
 *
 * <p>该类只负责 ORM 映射，不承载归档、租户隔离等业务规则。
 * 领域对象和数据库实体分离后，未来更换 ORM 或存储方案时影响更小。</p>
 */
@Entity
@Table(
        name = "knowledge_bases",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_knowledge_base_tenant_name",
                columnNames = {"tenant_id", "name"}
        )
)
public class KnowledgeBaseEntity {

    @Id
    @Column(length = 36, nullable = false, updatable = false)
    private String id;

    @Column(name = "tenant_id", length = 64, nullable = false)
    private String tenantId;

    @Column(length = 100, nullable = false)
    private String name;

    @Column(length = 500)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private KnowledgeBaseStatus status;

    @Column(name = "created_by", length = 64, nullable = false)
    private String createdBy;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected KnowledgeBaseEntity() {
        // JPA 需要无参构造方法。
    }

    private KnowledgeBaseEntity(
            String id,
            String tenantId,
            String name,
            String description,
            KnowledgeBaseStatus status,
            String createdBy,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        this.id = id;
        this.tenantId = tenantId;
        this.name = name;
        this.description = description;
        this.status = status;
        this.createdBy = createdBy;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static KnowledgeBaseEntity from(KnowledgeBase knowledgeBase) {
        return new KnowledgeBaseEntity(
                knowledgeBase.getId(),
                knowledgeBase.getTenantId(),
                knowledgeBase.getName(),
                knowledgeBase.getDescription(),
                knowledgeBase.getStatus(),
                knowledgeBase.getCreatedBy(),
                knowledgeBase.getCreatedAt(),
                knowledgeBase.getUpdatedAt()
        );
    }

    public KnowledgeBase toDomain() {
        return KnowledgeBase.reconstitute(
                id,
                tenantId,
                name,
                description,
                status,
                createdBy,
                createdAt,
                updatedAt
        );
    }

    public String getId() {
        return id;
    }
}
