package com.agentplatform.backend.document.infrastructure.persistence.entity;

import com.agentplatform.backend.document.domain.Document;
import com.agentplatform.backend.document.domain.DocumentStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * 文档数据库实体。
 */
@Entity
@Table(name = "documents")
public class DocumentEntity {

    @Id
    @Column(length = 36, nullable = false, updatable = false)
    private String id;

    @Column(name = "tenant_id", length = 64, nullable = false)
    private String tenantId;

    @Column(name = "knowledge_base_id", length = 36, nullable = false)
    private String knowledgeBaseId;

    @Column(name = "file_name", length = 255, nullable = false)
    private String fileName;

    @Column(name = "file_type", length = 32, nullable = false)
    private String fileType;

    @Column(name = "file_size", nullable = false)
    private long fileSize;

    @Column(name = "storage_path", length = 500, nullable = false)
    private String storagePath;

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private DocumentStatus status;

    @Column(name = "failure_reason", length = 1000)
    private String failureReason;

    @Column(name = "uploaded_by", length = 64, nullable = false)
    private String uploadedBy;

    @Column(name = "uploaded_at", nullable = false)
    private LocalDateTime uploadedAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected DocumentEntity() {
        // JPA 需要无参构造方法。
    }

    private DocumentEntity(
            String id,
            String tenantId,
            String knowledgeBaseId,
            String fileName,
            String fileType,
            long fileSize,
            String storagePath,
            DocumentStatus status,
            String failureReason,
            String uploadedBy,
            LocalDateTime uploadedAt,
            LocalDateTime updatedAt
    ) {
        this.id = id;
        this.tenantId = tenantId;
        this.knowledgeBaseId = knowledgeBaseId;
        this.fileName = fileName;
        this.fileType = fileType;
        this.fileSize = fileSize;
        this.storagePath = storagePath;
        this.status = status;
        this.failureReason = failureReason;
        this.uploadedBy = uploadedBy;
        this.uploadedAt = uploadedAt;
        this.updatedAt = updatedAt;
    }

    public static DocumentEntity from(Document document) {
        return new DocumentEntity(
                document.getId(),
                document.getTenantId(),
                document.getKnowledgeBaseId(),
                document.getFileName(),
                document.getFileType(),
                document.getFileSize(),
                document.getStoragePath(),
                document.getStatus(),
                document.getFailureReason(),
                document.getUploadedBy(),
                document.getUploadedAt(),
                document.getUpdatedAt()
        );
    }

    public Document toDomain() {
        return Document.reconstitute(
                id,
                tenantId,
                knowledgeBaseId,
                fileName,
                fileType,
                fileSize,
                storagePath,
                status,
                failureReason,
                uploadedBy,
                uploadedAt,
                updatedAt
        );
    }
}
