package com.agentplatform.backend.document.infrastructure.persistence.repository;

import com.agentplatform.backend.document.infrastructure.persistence.entity.DocumentEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Spring Data 对文档表的访问接口。
 */
public interface SpringDataDocumentRepository
        extends JpaRepository<DocumentEntity, String> {

    List<DocumentEntity> findAllByKnowledgeBaseIdOrderByUploadedAtDesc(
            String knowledgeBaseId
    );
}
