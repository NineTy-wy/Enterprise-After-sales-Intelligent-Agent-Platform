package com.agentplatform.backend.document.infrastructure.persistence.repository;

import com.agentplatform.backend.document.domain.Document;
import com.agentplatform.backend.document.domain.DocumentRepository;
import com.agentplatform.backend.document.infrastructure.persistence.entity.DocumentEntity;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * PostgreSQL 文档仓储适配器。
 */
@Repository
@Profile("postgres")
public class PostgresDocumentRepository implements DocumentRepository {

    private final SpringDataDocumentRepository repository;

    public PostgresDocumentRepository(SpringDataDocumentRepository repository) {
        this.repository = repository;
    }

    @Override
    public Document save(Document document) {
        return repository.save(DocumentEntity.from(document)).toDomain();
    }

    @Override
    public Optional<Document> findById(String id) {
        return repository.findById(id).map(DocumentEntity::toDomain);
    }

    @Override
    public List<Document> findByKnowledgeBaseId(String knowledgeBaseId) {
        return repository.findAllByKnowledgeBaseIdOrderByUploadedAtDesc(knowledgeBaseId)
                .stream()
                .map(DocumentEntity::toDomain)
                .toList();
    }
}
