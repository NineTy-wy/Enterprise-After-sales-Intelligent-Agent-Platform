package com.agentplatform.backend.knowledge.infrastructure.persistence.repository;

import com.agentplatform.backend.knowledge.infrastructure.persistence.entity.KnowledgeBaseEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Spring Data 对知识库表的访问接口。
 */
public interface SpringDataKnowledgeBaseRepository
        extends JpaRepository<KnowledgeBaseEntity, String> {

    List<KnowledgeBaseEntity> findAllByTenantIdOrderByCreatedAtDesc(String tenantId);

    boolean existsByTenantIdAndName(String tenantId, String name);
}
