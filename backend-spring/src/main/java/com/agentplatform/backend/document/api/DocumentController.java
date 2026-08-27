package com.agentplatform.backend.document.api;

import com.agentplatform.backend.audit.application.AuditService;
import com.agentplatform.backend.common.api.ApiResponse;
import com.agentplatform.backend.common.security.CurrentUser;
import com.agentplatform.backend.common.security.CurrentUserProvider;
import com.agentplatform.backend.document.api.dto.CreateDocumentRequest;
import com.agentplatform.backend.document.api.dto.DocumentResponse;
import com.agentplatform.backend.document.application.DocumentService;
import com.agentplatform.backend.document.application.DocumentUploadService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.agentplatform.backend.document.api.dto.UpdateDocumentStatusRequest;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;



import java.util.List;

/**
 * 文档接口控制器。
 *
 * <p>当前阶段提供文档元数据登记和查询能力。
 * 后续会扩展为真实文件上传接口，并触发异步解析任务。</p>
 */
@RestController
@RequestMapping("/api")
public class DocumentController {

    /** 文档应用服务，承载文档登记和文档查询业务用例。 */
    private final DocumentService documentService;

    /** 文件上传应用服务，负责保存文件并触发异步处理。 */
    private final DocumentUploadService documentUploadService;

    /** 当前用户上下文，安全关闭时会返回本地演示身份。 */
    private final CurrentUserProvider currentUserProvider;

    /** 记录上传和状态回调等关键操作。 */
    private final AuditService auditService;

    /**
     * 当前阶段用于模拟 FastAPI Agent 服务身份。
     *
     * <p>后续接入服务间认证后，会替换为真实的服务身份。</p>
     */
    private static final String DEMO_AGENT_OPERATOR_ID = "agent_service_demo";

    /**
     * 使用构造方法注入 Service，保持依赖清晰。
     */
    public DocumentController(
            DocumentService documentService,
            DocumentUploadService documentUploadService,
            CurrentUserProvider currentUserProvider,
            AuditService auditService
    ) {
        this.documentService = documentService;
        this.documentUploadService = documentUploadService;
        this.currentUserProvider = currentUserProvider;
        this.auditService = auditService;
    }

    /**
     * 在指定知识库下创建文档记录。
     *
     * <p>这里仍然使用 JSON 登记文档元数据；
     * 后续接真实文件上传时会改成 multipart/form-data。</p>
     */
    @PostMapping("/documents")
    public ApiResponse<DocumentResponse> createDocument(
            @Valid @RequestBody CreateDocumentRequest request
    ) {
        CurrentUser currentUser = currentUserProvider.currentUser();
        DocumentResponse response = documentService.createDocument(
                currentUser.tenantId(),
                currentUser.userId(),
                request
        );
        auditService.record(
                currentUser.tenantId(),
                currentUser.userId(),
                "CREATE",
                "DOCUMENT",
                response.id(),
                "{\"fileName\":\"" + request.fileName() + "\"}"
        );
        return ApiResponse.success(response);
    }

    /**
     * 上传真实文件并创建文档记录。
     *
     * <p>企业项目里文件上传通常先进入业务后端，后端保存到对象存储，
     * 再发布异步任务给 Agent 服务处理解析、切分和向量化。</p>
     */
    @PostMapping("/knowledge-bases/{knowledgeBaseId}/documents/upload")
    public ApiResponse<DocumentResponse> uploadDocument(
            @PathVariable String knowledgeBaseId,
            @RequestParam("file") MultipartFile file
    ) {
        CurrentUser currentUser = currentUserProvider.currentUser();
        DocumentResponse response = documentUploadService.uploadDocument(
                currentUser.tenantId(),
                currentUser.userId(),
                knowledgeBaseId,
                file
        );
        auditService.record(
                currentUser.tenantId(),
                currentUser.userId(),
                "UPLOAD",
                "DOCUMENT",
                response.id(),
                "{\"fileName\":\"" + response.fileName() + "\"}"
        );
        return ApiResponse.success(response);
    }

    /**
     * 查询某个知识库下的文档列表。
     */
    @GetMapping("/knowledge-bases/{knowledgeBaseId}/documents")
    public ApiResponse<List<DocumentResponse>> listDocuments(
            @PathVariable String knowledgeBaseId
    ) {
        CurrentUser currentUser = currentUserProvider.currentUser();
        List<DocumentResponse> responses = documentService.listDocuments(
                currentUser.tenantId(),
                knowledgeBaseId
        );
        return ApiResponse.success(responses);
    }

    /**
     * 更新文档处理状态。
     *
     * <p>后续由 FastAPI Agent 服务调用，用于回传文档解析、
     * Chunk 切分、Embedding 和向量入库的处理结果。</p>
     */
    @PatchMapping("/documents/{documentId}/status")
    public ApiResponse<DocumentResponse> updateDocumentStatus(
            @PathVariable String documentId,
            @Valid @RequestBody UpdateDocumentStatusRequest request
    ) {
        CurrentUser currentUser = currentUserProvider.currentUser();
        DocumentResponse response = documentService.updateDocumentStatus(
                currentUser.tenantId(),
                DEMO_AGENT_OPERATOR_ID,
                documentId,
                request
        );
        auditService.record(
                currentUser.tenantId(),
                currentUser.userId(),
                "UPDATE_STATUS",
                "DOCUMENT",
                response.id(),
                "{\"status\":\"" + request.status().name() + "\"}"
        );
        return ApiResponse.success(response);
    }
}
