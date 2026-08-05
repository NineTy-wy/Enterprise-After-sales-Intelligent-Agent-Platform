package com.agentplatform.backend.document.application.impl;

import com.agentplatform.backend.common.error.BusinessException;
import com.agentplatform.backend.common.error.ErrorCode;
import com.agentplatform.backend.document.api.dto.CreateDocumentRequest;
import com.agentplatform.backend.document.api.dto.DocumentResponse;
import com.agentplatform.backend.document.application.DocumentService;
import com.agentplatform.backend.document.domain.Document;
import com.agentplatform.backend.document.domain.DocumentRepository;
import com.agentplatform.backend.knowledge.api.dto.KnowledgeBaseResponse;
import com.agentplatform.backend.knowledge.application.KnowledgeBaseService;
import com.agentplatform.backend.knowledge.domain.KnowledgeBaseStatus;
import org.springframework.stereotype.Service;
import com.agentplatform.backend.document.api.dto.UpdateDocumentStatusRequest;




import java.util.List;

/**
 * 文档应用服务实现。
 *
 * <p>负责文档登记、文档列表查询等业务用例。
 * 当前阶段只保存文档元数据，后续会扩展真实文件上传、
 * MinIO 存储、RabbitMQ 异步解析和向量化入库。</p>
 */
@Service
public class DocumentServiceImpl implements DocumentService {

    /** 文档仓储接口，当前注入内存实现，后续可替换为 PostgreSQL 实现。 */
    private final DocumentRepository documentRepository;

    /** 知识库服务，用于校验目标知识库是否存在、是否属于当前租户。 */
    private final KnowledgeBaseService knowledgeBaseService;

    /**
     * 使用构造方法注入依赖，保证依赖关系清晰。
     */
    public DocumentServiceImpl(
            DocumentRepository documentRepository,
            KnowledgeBaseService knowledgeBaseService
    ) {
        this.documentRepository = documentRepository;
        this.knowledgeBaseService = knowledgeBaseService;
    }

    @Override
    public DocumentResponse createDocument(
            String tenantId,
            String userId,
            CreateDocumentRequest request
    ) {
        KnowledgeBaseResponse knowledgeBase = knowledgeBaseService.getKnowledgeBase(
                tenantId,
                request.knowledgeBaseId()
        );

        if (KnowledgeBaseStatus.ARCHIVED.equals(knowledgeBase.status())) {
            throw new BusinessException(
                    ErrorCode.RESOURCE_STATE_CONFLICT,
                    "已归档知识库不能上传文档"
            );
        }

        Document document = Document.createUploaded(
                tenantId,
                request.knowledgeBaseId(),
                request.fileName(),
                request.fileType(),
                request.fileSize(),
                request.storagePath(),
                userId
        );

        Document savedDocument = documentRepository.save(document);
        return DocumentResponse.from(savedDocument);
    }

    @Override
    public List<DocumentResponse> listDocuments(
            String tenantId,
            String knowledgeBaseId
    ) {
        knowledgeBaseService.getKnowledgeBase(tenantId, knowledgeBaseId);

        return documentRepository.findByKnowledgeBaseId(knowledgeBaseId)
                .stream()
                .map(DocumentResponse::from)
                .toList();
    }
    @Override
    public DocumentResponse updateDocumentStatus(
            String tenantId,
            String operatorId,
            String documentId,
            UpdateDocumentStatusRequest request
    ) {
        Document document = documentRepository.findById(documentId)
                .filter(item -> tenantId.equals(item.getTenantId()))
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.RESOURCE_NOT_FOUND,
                        "文档不存在"
                ));

        if (request.status() == null) {
            throw new BusinessException(
                    ErrorCode.INVALID_REQUEST,
                    "文档状态不能为空"
            );
        }

        try {
            switch (request.status()) {
                case PROCESSING -> document.markProcessing();

                case INDEXED -> document.markIndexed();

                case FAILED -> {
                    if (request.failureReason() == null
                            || request.failureReason().isBlank()) {
                        throw new BusinessException(
                                ErrorCode.INVALID_REQUEST,
                                "文档处理失败时必须提供失败原因"
                        );
                    }

                    document.markFailed(request.failureReason());
                }

                case UPLOADED -> throw new BusinessException(
                        ErrorCode.RESOURCE_STATE_CONFLICT,
                        "文档不能回退到已上传状态"
                );
            }
        } catch (IllegalStateException exception) {
            throw new BusinessException(
                    ErrorCode.RESOURCE_STATE_CONFLICT,
                    exception.getMessage()
            );
        }

        Document savedDocument = documentRepository.save(document);
        return DocumentResponse.from(savedDocument);
    }
}