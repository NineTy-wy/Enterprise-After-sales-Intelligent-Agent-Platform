package com.agentplatform.backend.knowledge.infrastructure.repository;

import com.agentplatform.backend.knowledge.domain.KnowledgeBase;
import com.agentplatform.backend.knowledge.domain.KnowledgeBaseRepository;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 知识库仓储的内存实现。
 *
 * <p>该实现用于项目早期开发阶段，让 Controller 和 Service 可以先跑通。
 * 后续接入 PostgreSQL 后，只需要新增数据库实现并替换 Bean 即可。</p>
 */
@Repository
public class InMemoryKnowledgeBaseRepository implements KnowledgeBaseRepository {

    /**
     * 使用线程安全 Map 模拟数据库表。
     *
     * <p>Key 是知识库 ID，Value 是知识库实体。
     * ConcurrentHashMap 可以避免并发请求下普通 HashMap 的线程安全问题。</p>
     */
    private final ConcurrentMap<String, KnowledgeBase> storage = new ConcurrentHashMap<>();

    @Override
    public KnowledgeBase save(KnowledgeBase knowledgeBase) {
        storage.put(knowledgeBase.getId(), knowledgeBase);
        return knowledgeBase;
    }

    @Override
    public Optional<KnowledgeBase> findById(String id) {
        return Optional.ofNullable(storage.get(id));
    }

    @Override
    public List<KnowledgeBase> findByTenantId(String tenantId) {
        return storage.values()
                .stream()
                .filter(knowledgeBase -> tenantId.equals(knowledgeBase.getTenantId()))
                .toList();
    }

    @Override
    public boolean existsByTenantIdAndName(String tenantId, String name) {
        return storage.values()
                .stream()
                .anyMatch(knowledgeBase ->
                        tenantId.equals(knowledgeBase.getTenantId())
                                && name.equals(knowledgeBase.getName())
                );
    }
}
