package com.agentplatform.backend.document.domain;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * 文档领域实体。
 *
 * <p>它代表上传到某个知识库中的一份业务文档，
 * 例如 PDF 维修手册、Word 售后流程、Excel 历史工单等。</p>
 */
public class Document {

    /** 文档唯一标识，后续作为数据库主键和接口返回 ID。 */
    private String id;

    /** 租户 ID，用于多租户数据隔离。 */
    private String tenantId;

    /** 所属知识库 ID。 */
    private String knowledgeBaseId;

    /** 原始文件名，例如 A100维修手册.pdf。 */
    private String fileName;

    /** 文件类型，例如 pdf、docx、xlsx。 */
    private String fileType;

    /** 文件大小，单位为字节。 */
    private long fileSize;

    /** 文件存储路径，后续可对应 MinIO object key 或本地临时路径。 */
    private String storagePath;

    /** 文档处理状态。 */
    private DocumentStatus status;

    /** 处理失败原因，仅在 FAILED 状态下有值。 */
    private String failureReason;

    /** 上传人用户 ID，用于审计和权限判断。 */
    private String uploadedBy;

    /** 上传时间。 */
    private LocalDateTime uploadedAt;

    /** 更新时间。 */
    private LocalDateTime updatedAt;

    /**
     * 保留无参构造方法，方便后续 ORM、JSON 序列化框架使用。
     */
    public Document() {
    }

    /**
     * 创建已上传状态的文档记录。
     *
     * <p>此时文件已经被业务后端接收，但还没有进入解析和向量化流程。</p>
     */
    public static Document createUploaded(
            String tenantId,
            String knowledgeBaseId,
            String fileName,
            String fileType,
            long fileSize,
            String storagePath,
            String uploadedBy
    ) {
        LocalDateTime now = LocalDateTime.now();

        Document document = new Document();
        document.id = UUID.randomUUID().toString();
        document.tenantId = tenantId;
        document.knowledgeBaseId = knowledgeBaseId;
        document.fileName = fileName;
        document.fileType = fileType;
        document.fileSize = fileSize;
        document.storagePath = storagePath;
        document.status = DocumentStatus.UPLOADED;
        document.uploadedBy = uploadedBy;
        document.uploadedAt = now;
        document.updatedAt = now;
        return document;
    }

    /**
     * 将文档标记为处理中。
     */
    public void markProcessing() {
        ensureStatus(DocumentStatus.UPLOADED, "只有已上传的文档才能进入处理中状态");
        this.status = DocumentStatus.PROCESSING;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 将文档标记为已完成索引。
     */
    public void markIndexed() {
        ensureStatus(DocumentStatus.PROCESSING, "只有处理中的文档才能标记为已入库");
        this.status = DocumentStatus.INDEXED;
        this.failureReason = null;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 将文档标记为处理失败。
     */
    public void markFailed(String failureReason) {
        if (DocumentStatus.INDEXED.equals(this.status)) {
            throw new IllegalStateException("已入库文档不能标记为失败");
        }

        this.status = DocumentStatus.FAILED;
        this.failureReason = failureReason;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 校验当前状态是否符合预期。
     */
    private void ensureStatus(DocumentStatus expectedStatus, String errorMessage) {
        if (!Objects.equals(this.status, expectedStatus)) {
            throw new IllegalStateException(errorMessage);
        }
    }

    public String getId() {
        return id;
    }

    public String getTenantId() {
        return tenantId;
    }

    public String getKnowledgeBaseId() {
        return knowledgeBaseId;
    }

    public String getFileName() {
        return fileName;
    }

    public String getFileType() {
        return fileType;
    }

    public long getFileSize() {
        return fileSize;
    }

    public String getStoragePath() {
        return storagePath;
    }

    public DocumentStatus getStatus() {
        return status;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public String getUploadedBy() {
        return uploadedBy;
    }

    public LocalDateTime getUploadedAt() {
        return uploadedAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}