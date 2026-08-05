package com.agentplatform.backend.knowledge.api.dto;

import com.agentplatform.backend.knowledge.domain.KnowledgeBase;
import com.agentplatform.backend.knowledge.domain.KnowledgeBaseStatus;

import java.time.LocalDateTime;

/**
 * 知识库接口响应对象。
 *
 * <p>Response DTO 用于控制接口对外暴露的数据结构。
 * 领域实体 KnowledgeBase 不直接返回给前端，避免未来内部字段泄露。</p>
 */
public record KnowledgeBaseResponse(

        /** 知识库 ID。 */
        String id,

        /** 知识库名称。 */
        String name,

        /** 知识库描述。 */
        String description,

        /** 知识库状态。 */
        KnowledgeBaseStatus status,

        /** 创建时间。 */
        LocalDateTime createdAt,

        /** 更新时间。 */
        LocalDateTime updatedAt
) {

    /**
     * 将领域实体转换为接口响应对象。
     *
     * <p>这里没有暴露 tenantId、createdBy 等内部字段，
     * 是因为这些字段属于权限和审计上下文，不一定适合直接返回给前端。</p>
     */
    public static KnowledgeBaseResponse from(KnowledgeBase knowledgeBase) {
        return new KnowledgeBaseResponse(
                knowledgeBase.getId(),
                knowledgeBase.getName(),
                knowledgeBase.getDescription(),
                knowledgeBase.getStatus(),
                knowledgeBase.getCreatedAt(),
                knowledgeBase.getUpdatedAt()
        );
    }
}