package com.agentplatform.backend.document.api;

import com.agentplatform.backend.common.api.ApiResponse;
import com.agentplatform.backend.document.api.dto.CreateDocumentRequest;
import com.agentplatform.backend.document.api.dto.DocumentResponse;
import com.agentplatform.backend.document.application.DocumentService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.agentplatform.backend.document.api.dto.UpdateDocumentStatusRequest;
import org.springframework.web.bind.annotation.PatchMapping;



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

    /** 当前阶段使用固定租户 ID，后续接入登录后从 JWT 中获取。 */
    private static final String DEMO_TENANT_ID = "tenant_demo";

    /** 当前阶段使用固定用户 ID，后续接入登录后从认证上下文中获取。 */
    private static final String DEMO_USER_ID = "user_demo";

    /** 文档应用服务，承载文档登记和文档查询业务用例。 */
    private final DocumentService documentService;

    /**
     * 当前阶段用于模拟 FastAPI Agent 服务身份。
     *
     * <p>后续接入服务间认证后，会替换为真实的服务身份。</p>
     */
    private static final String DEMO_AGENT_OPERATOR_ID = "agent_service_demo";

    /**
     * 使用构造方法注入 Service，保持依赖清晰。
     */
    public DocumentController(DocumentService documentService) {
        this.documentService = documentService;
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
        DocumentResponse response = documentService.createDocument(
                DEMO_TENANT_ID,
                DEMO_USER_ID,
                request
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
        List<DocumentResponse> responses = documentService.listDocuments(
                DEMO_TENANT_ID,
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
        DocumentResponse response = documentService.updateDocumentStatus(
                DEMO_TENANT_ID,
                DEMO_AGENT_OPERATOR_ID,
                documentId,
                request
        );
        return ApiResponse.success(response);
    }
}