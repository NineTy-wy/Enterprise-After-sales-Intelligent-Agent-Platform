package com.agentplatform.backend.document.application;

import com.agentplatform.backend.document.api.dto.DocumentResponse;
import com.agentplatform.backend.document.application.FileStorage.StoredFile;
import com.agentplatform.backend.document.api.dto.UpdateDocumentStatusRequest;
import com.agentplatform.backend.document.domain.DocumentStatus;
import com.agentplatform.backend.document.domain.Document;
import com.agentplatform.backend.knowledge.api.dto.KnowledgeBaseResponse;
import com.agentplatform.backend.knowledge.application.KnowledgeBaseService;
import com.agentplatform.backend.knowledge.domain.KnowledgeBaseStatus;
import com.agentplatform.backend.common.error.BusinessException;
import com.agentplatform.backend.common.error.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.Locale;

/**
 * 真实文件上传用例。
 */
@Service
public class DocumentUploadService {

    private final KnowledgeBaseService knowledgeBaseService;
    private final DocumentService documentService;
    private final FileStorage fileStorage;
    private final DocumentProcessingPublisher processingPublisher;

    public DocumentUploadService(
            KnowledgeBaseService knowledgeBaseService,
            DocumentService documentService,
            FileStorage fileStorage,
            DocumentProcessingPublisher processingPublisher
    ) {
        this.knowledgeBaseService = knowledgeBaseService;
        this.documentService = documentService;
        this.fileStorage = fileStorage;
        this.processingPublisher = processingPublisher;
    }

    public DocumentResponse uploadDocument(
            String tenantId,
            String userId,
            String knowledgeBaseId,
            MultipartFile file
    ) {
        KnowledgeBaseResponse knowledgeBase =
                knowledgeBaseService.getKnowledgeBase(tenantId, knowledgeBaseId);
        if (knowledgeBase.status() == KnowledgeBaseStatus.ARCHIVED) {
            throw new BusinessException(
                    ErrorCode.RESOURCE_STATE_CONFLICT,
                    "已归档知识库不能上传文档"
            );
        }

        String fileName = file == null ? "" : file.getOriginalFilename();
        if (fileName == null || fileName.isBlank()) {
            throw new BusinessException(
                    ErrorCode.INVALID_REQUEST,
                    "上传文件名不能为空"
            );
        }
        String fileType = extractFileType(fileName, file);
        validateSupportedFileType(fileType);

        StoredFile storedFile = fileStorage.store(
                tenantId,
                knowledgeBaseId,
                file
        );

        DocumentResponse document = null;
        try {
            document = documentService.createDocument(
                    tenantId,
                    userId,
                    new com.agentplatform.backend.document.api.dto.CreateDocumentRequest(
                            knowledgeBaseId,
                            fileName,
                            fileType,
                            storedFile.size(),
                            storedFile.objectKey()
                    )
            );

            processingPublisher.publish(new DocumentProcessingMessage(
                    document.id(),
                    tenantId,
                    knowledgeBaseId,
                    document.fileName(),
                    document.fileType(),
                    storedFile.objectKey()
            ));

            return document;
        } catch (RuntimeException exception) {
            if (document != null) {
                try {
                    documentService.updateDocumentStatus(
                            tenantId,
                            "document-upload",
                            document.id(),
                            new UpdateDocumentStatusRequest(
                                    DocumentStatus.FAILED,
                                    "文档任务发布失败，请稍后重试"
                            )
                    );
                } catch (RuntimeException stateException) {
                    exception.addSuppressed(stateException);
                }
            }
            try {
                fileStorage.delete(storedFile.objectKey());
            } catch (RuntimeException cleanupException) {
                exception.addSuppressed(cleanupException);
            }
            throw exception;
        }
    }

    private String extractFileType(String fileName, MultipartFile file) {
        if (fileName != null && fileName.contains(".")) {
            return fileName.substring(fileName.lastIndexOf('.') + 1)
                    .toLowerCase(Locale.ROOT);
        }
        if (file != null && file.getContentType() != null) {
            return file.getContentType().toLowerCase(Locale.ROOT);
        }
        return "unknown";
    }

    private void validateSupportedFileType(String fileType) {
        if (!java.util.Set.of(
                "pdf", "doc", "docx", "xls", "xlsx", "csv", "txt", "md"
        ).contains(fileType)) {
            throw new BusinessException(
                    ErrorCode.INVALID_REQUEST,
                    "暂不支持该文件类型，仅支持 PDF、Word、Excel、CSV、TXT 和 Markdown"
            );
        }
    }
}
