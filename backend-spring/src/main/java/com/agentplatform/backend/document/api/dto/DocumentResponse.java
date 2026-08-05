package com.agentplatform.backend.document.api.dto;

import com.agentplatform.backend.document.domain.Document;
import com.agentplatform.backend.document.domain.DocumentStatus;

import java.time.LocalDateTime;

/**
 * 文档接口响应对象。
 *
 * <p>用于向前端返回文档元数据和处理状态，
 * 不直接暴露领域实体的全部内部字段。</p>
 */
public record DocumentResponse(

        /** 文档 ID。 */
        String id,

        /** 所属知识库 ID。 */
        String knowledgeBaseId,

        /** 原始文件名。 */
        String fileName,

        /** 文件类型，例如 pdf、docx、xlsx。 */
        String fileType,

        /** 文件大小，单位为字节。 */
        long fileSize,

        /** 当前处理状态。 */
        DocumentStatus status,

        /** 处理失败原因；非失败状态下通常为 null。 */
        String failureReason,

        /** 文档上传时间。 */
        LocalDateTime uploadedAt,

        /** 文档信息最后更新时间。 */
        LocalDateTime updatedAt
) {

    /**
     * 将领域实体转换为接口响应对象。
     *
     * <p>不返回 tenantId、storagePath、uploadedBy 等内部字段，
     * 避免暴露租户信息、存储路径和审计信息。</p>
     */
    public static DocumentResponse from(Document document) {
        return new DocumentResponse(
                document.getId(),
                document.getKnowledgeBaseId(),
                document.getFileName(),
                document.getFileType(),
                document.getFileSize(),
                document.getStatus(),
                document.getFailureReason(),
                document.getUploadedAt(),
                document.getUpdatedAt()
        );
    }
}
