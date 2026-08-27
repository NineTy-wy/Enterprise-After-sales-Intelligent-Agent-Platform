package com.agentplatform.backend.knowledge.infrastructure.persistence.repository;

import com.agentplatform.backend.knowledge.domain.KnowledgeBase;
import com.agentplatform.backend.knowledge.domain.KnowledgeBaseRepository;
import com.agentplatform.backend.knowledge.infrastructure.persistence.entity.KnowledgeBaseEntity;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * PostgreSQL 知识库仓储适配器。
 *
 * <p>应用层依赖领域仓储接口，具体的 Spring Data 实现被隔离在基础设施层。</p>
 */
@Repository
@Profile("postgres")
public class PostgresKnowledgeBaseRepository implements KnowledgeBaseRepository {

    private final SpringDataKnowledgeBaseRepository repository;

    public PostgresKnowledgeBaseRepository(
            SpringDataKnowledgeBaseRepository repository
    ) {
        this.repository = repository;
    }

    @Override
    public KnowledgeBase save(KnowledgeBase knowledgeBase) {
        return repository.save(KnowledgeBaseEntity.from(knowledgeBase)).toDomain();
    }

    @Override
    public Optional<KnowledgeBase> findById(String id) {
        return repository.findById(id).map(KnowledgeBaseEntity::toDomain);
    }

    @Override
    public List<KnowledgeBase> findByTenantId(String tenantId) {
        return repository.findAllByTenantIdOrderByCreatedAtDesc(tenantId)
                .stream()
                .map(KnowledgeBaseEntity::toDomain)
                .toList();
    }

    @Override
    public boolean existsByTenantIdAndName(String tenantId, String name) {
        return repository.existsByTenantIdAndName(tenantId, name);
    }
}
