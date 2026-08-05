package com.agentplatform.backend.knowledge.domain;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * 知识库领域实体。
 *
 * <p>它代表业务中的一个知识库，例如“售后知识库”“设备维修手册知识库”。
 * 当前类先保持为纯 Java 对象，不直接绑定 JPA 或 MyBatis 注解，
 * 这样领域逻辑不会过早依赖具体数据库框架。</p>
 */
public class KnowledgeBase {

    /** 知识库唯一标识，后续会作为数据库主键和接口返回 ID。 */
    private String id;

    /** 租户 ID，用于后续支持企业级多租户数据隔离。 */
    private String tenantId;

    /** 知识库名称，例如“售后知识库”。 */
    private String name;

    /** 知识库描述，用于说明知识库用途和数据范围。 */
    private String description;

    /** 知识库状态，用于控制是否允许上传文档和检索。 */
    private KnowledgeBaseStatus status;

    /** 创建人用户 ID，用于审计和权限判断。 */
    private String createdBy;

    /** 创建时间。 */
    private LocalDateTime createdAt;

    /** 更新时间。 */
    private LocalDateTime updatedAt;

    /**
     * 保留无参构造方法，方便后续 ORM 框架、JSON 序列化框架使用。
     */
    public KnowledgeBase() {
    }

    /**
     * 创建一个新的知识库。
     *
     * <p>创建动作集中在这里，而不是散落在 Controller 或 Service 中，
     * 可以保证 ID、状态、创建时间等基础字段始终完整。</p>
     */
    public static KnowledgeBase create(
            String tenantId,
            String name,
            String description,
            String createdBy
    ) {
        KnowledgeBase knowledgeBase = new KnowledgeBase();
        knowledgeBase.id = UUID.randomUUID().toString();
        knowledgeBase.tenantId = tenantId;
        knowledgeBase.name = name;
        knowledgeBase.description = description;
        knowledgeBase.status = KnowledgeBaseStatus.ACTIVE;
        knowledgeBase.createdBy = createdBy;
        knowledgeBase.createdAt = LocalDateTime.now();
        knowledgeBase.updatedAt = knowledgeBase.createdAt;
        return knowledgeBase;
    }

    /**
     * 修改知识库基础信息。
     *
     * <p>这里没有直接暴露 setName、setDescription 给业务层随意调用，
     * 是为了把“修改知识库信息”这个业务动作表达清楚。</p>
     */
    public void updateProfile(String name, String description) {
        this.name = name;
        this.description = description;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 归档知识库。
     *
     * <p>归档不是物理删除，历史文档、问答记录、审计日志仍然保留。
     * 已归档的知识库不允许重复归档，避免产生无意义的状态操作。</p>
     */
    public void archive() {
        if (KnowledgeBaseStatus.ARCHIVED.equals(this.status)) {
            throw new IllegalStateException("知识库已归档，不能重复归档");
        }

        this.status = KnowledgeBaseStatus.ARCHIVED;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 判断知识库是否处于可用状态。
     */
    public boolean isActive() {
        return Objects.equals(this.status, KnowledgeBaseStatus.ACTIVE);
    }

    public String getId() {
        return id;
    }

    public String getTenantId() {
        return tenantId;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public KnowledgeBaseStatus getStatus() {
        return status;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
