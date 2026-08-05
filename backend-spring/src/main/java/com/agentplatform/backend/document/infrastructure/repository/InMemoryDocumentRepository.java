package com.agentplatform.backend.document.infrastructure.repository;

import com.agentplatform.backend.document.domain.Document;
import com.agentplatform.backend.document.domain.DocumentRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 文档仓储的内存实现。
 *
 * <p>用于项目早期开发阶段，先跑通文档业务流程。
 * 后续接入 PostgreSQL 后，可新增数据库实现替换当前 Bean。</p>
 */
@Repository
public class InMemoryDocumentRepository implements DocumentRepository {

    /**
     * 使用线程安全 Map 模拟文档表。
     *
     * <p>Key 是文档 ID，Value 是文档实体。</p>
     */
    private final ConcurrentMap<String, Document> storage = new ConcurrentHashMap<>();

    @Override
    public Document save(Document document) {
        storage.put(document.getId(), document);
        return document;
    }

    @Override
    public Optional<Document> findById(String id) {
        return Optional.ofNullable(storage.get(id));
    }

    @Override
    public List<Document> findByKnowledgeBaseId(String knowledgeBaseId) {
        return storage.values()
                .stream()
                .filter(document -> knowledgeBaseId.equals(document.getKnowledgeBaseId()))
                .toList();
    }
}