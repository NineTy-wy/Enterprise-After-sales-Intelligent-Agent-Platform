package com.agentplatform.backend.document.application.impl;

import com.agentplatform.backend.common.error.BusinessException;
import com.agentplatform.backend.common.error.ErrorCode;
import com.agentplatform.backend.document.api.dto.CreateDocumentRequest;
import com.agentplatform.backend.document.api.dto.UpdateDocumentStatusRequest;
import com.agentplatform.backend.document.api.dto.DocumentResponse;
import com.agentplatform.backend.document.application.DocumentService;
import com.agentplatform.backend.document.domain.DocumentStatus;
import com.agentplatform.backend.document.infrastructure.repository.InMemoryDocumentRepository;
import com.agentplatform.backend.knowledge.api.dto.CreateKnowledgeBaseRequest;
import com.agentplatform.backend.knowledge.api.dto.KnowledgeBaseResponse;
import com.agentplatform.backend.knowledge.application.KnowledgeBaseService;
import com.agentplatform.backend.knowledge.application.impl.KnowledgeBaseServiceImpl;
import com.agentplatform.backend.knowledge.infrastructure.repository.InMemoryKnowledgeBaseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * DocumentServiceImpl 单元测试。
 *
 * <p>该测试验证文档应用服务的核心业务规则，
 * 不启动 Spring Boot，也不依赖真实数据库或文件存储。</p>
 */
class DocumentServiceImplTest {

    /** 测试租户 ID，用于模拟当前登录用户所属租户。 */
    private static final String TEST_TENANT_ID = "tenant_test";

    /** 测试用户 ID，用于模拟当前操作人。 */
    private static final String TEST_USER_ID = "user_test";

    /** 知识库服务，用于创建测试知识库。 */
    private KnowledgeBaseService knowledgeBaseService;

    /** 被测试的文档服务。 */
    private DocumentService documentService;

    /**
     * 每个测试执行前重新创建内存仓储和服务，避免测试数据互相影响。
     */
    @BeforeEach
    void setUp() {
        knowledgeBaseService = new KnowledgeBaseServiceImpl(
                new InMemoryKnowledgeBaseRepository()
        );

        documentService = new DocumentServiceImpl(
                new InMemoryDocumentRepository(),
                knowledgeBaseService
        );
    }

    /**
     * 验证文档创建后状态为 UPLOADED。
     */
    @Test
    void createDocument_shouldCreateUploadedDocument() {
        KnowledgeBaseResponse knowledgeBase = createKnowledgeBase();

        CreateDocumentRequest request = new CreateDocumentRequest(
                knowledgeBase.id(),
                "A100维修手册.pdf",
                "pdf",
                204800,
                "tenant_test/knowledge-base/A100维修手册.pdf"
        );

        DocumentResponse response = documentService.createDocument(
                TEST_TENANT_ID,
                TEST_USER_ID,
                request
        );

        assertNotNull(response.id());
        assertEquals(knowledgeBase.id(), response.knowledgeBaseId());
        assertEquals("A100维修手册.pdf", response.fileName());
        assertEquals("pdf", response.fileType());
        assertEquals(204800, response.fileSize());
        assertEquals(DocumentStatus.UPLOADED, response.status());
        assertNotNull(response.uploadedAt());
        assertNotNull(response.updatedAt());
    }

    /**
     * 验证已归档知识库不能继续创建文档。
     *
     * <p>知识库归档后代表退出正常业务流程，
     * 文档模块必须尊重知识库状态，禁止继续写入文档。</p>
     */
    @Test
    void createDocument_shouldRejectArchivedKnowledgeBase() {
        KnowledgeBaseResponse knowledgeBase = createKnowledgeBase();

        knowledgeBaseService.archiveKnowledgeBase(
                TEST_TENANT_ID,
                TEST_USER_ID,
                knowledgeBase.id()
        );

        CreateDocumentRequest request = new CreateDocumentRequest(
                knowledgeBase.id(),
                "归档后上传.pdf",
                "pdf",
                1024,
                "tenant_test/knowledge-base/archived.pdf"
        );

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> documentService.createDocument(
                        TEST_TENANT_ID,
                        TEST_USER_ID,
                        request
                )
        );

        assertEquals(ErrorCode.RESOURCE_STATE_CONFLICT, exception.getErrorCode());
    }


    /**
     * 验证跨租户不能查询其他租户知识库下的文档列表。
     *
     * <p>文档列表查询前会先校验知识库归属，
     * 如果知识库不属于当前租户，应统一返回资源不存在。</p>
     */
    @Test
    void listDocuments_shouldRejectOtherTenantKnowledgeBase() {
        KnowledgeBaseResponse knowledgeBase = createKnowledgeBase();

        CreateDocumentRequest request = new CreateDocumentRequest(
                knowledgeBase.id(),
                "租户隔离文档.pdf",
                "pdf",
                4096,
                "tenant_test/knowledge-base/tenant-isolation.pdf"
        );

        documentService.createDocument(
                TEST_TENANT_ID,
                TEST_USER_ID,
                request
        );

        String otherTenantId = "tenant_other";

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> documentService.listDocuments(
                        otherTenantId,
                        knowledgeBase.id()
                )
        );

        assertEquals(ErrorCode.RESOURCE_NOT_FOUND, exception.getErrorCode());
    }

    /**
     * 验证文档可以从 UPLOADED 流转到 PROCESSING，再流转到 INDEXED。
     *
     * <p>这是后续 FastAPI Agent 服务解析文档并完成向量入库后的核心回调流程。</p>
     */
    @Test
    void updateDocumentStatus_shouldMarkDocumentAsIndexed() {
        KnowledgeBaseResponse knowledgeBase = createKnowledgeBase();

        DocumentResponse document = documentService.createDocument(
                TEST_TENANT_ID,
                TEST_USER_ID,
                new CreateDocumentRequest(
                        knowledgeBase.id(),
                        "状态流转测试.pdf",
                        "pdf",
                        204800,
                        "tenant_test/knowledge-base/status-flow.pdf"
                )
        );

        DocumentResponse processingDocument = documentService.updateDocumentStatus(
                TEST_TENANT_ID,
                "agent_service_test",
                document.id(),
                new UpdateDocumentStatusRequest(
                        DocumentStatus.PROCESSING,
                        null
                )
        );

        assertEquals(DocumentStatus.PROCESSING, processingDocument.status());

        DocumentResponse indexedDocument = documentService.updateDocumentStatus(
                TEST_TENANT_ID,
                "agent_service_test",
                document.id(),
                new UpdateDocumentStatusRequest(
                        DocumentStatus.INDEXED,
                        null
                )
        );

        assertEquals(DocumentStatus.INDEXED, indexedDocument.status());
    }

    /**
     * 验证已入库文档不能回退到处理中状态。
     *
     * <p>非法状态流转应转换为 RESOURCE_STATE_CONFLICT，
     * 不能被当作系统内部异常。</p>
     */
    @Test
    void updateDocumentStatus_shouldRejectInvalidStateTransition() {
        KnowledgeBaseResponse knowledgeBase = createKnowledgeBase();

        DocumentResponse document = documentService.createDocument(
                TEST_TENANT_ID,
                TEST_USER_ID,
                new CreateDocumentRequest(
                        knowledgeBase.id(),
                        "非法状态流转测试.pdf",
                        "pdf",
                        1024,
                        "tenant_test/knowledge-base/invalid-flow.pdf"
                )
        );

        documentService.updateDocumentStatus(
                TEST_TENANT_ID,
                "agent_service_test",
                document.id(),
                new UpdateDocumentStatusRequest(
                        DocumentStatus.PROCESSING,
                        null
                )
        );

        documentService.updateDocumentStatus(
                TEST_TENANT_ID,
                "agent_service_test",
                document.id(),
                new UpdateDocumentStatusRequest(
                        DocumentStatus.INDEXED,
                        null
                )
        );

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> documentService.updateDocumentStatus(
                        TEST_TENANT_ID,
                        "agent_service_test",
                        document.id(),
                        new UpdateDocumentStatusRequest(
                                DocumentStatus.PROCESSING,
                                null
                        )
                )
        );

        assertEquals(
                ErrorCode.RESOURCE_STATE_CONFLICT,
                exception.getErrorCode()
        );
    }

    /**
     * 创建测试知识库，供文档测试用例复用。
     */
    private KnowledgeBaseResponse createKnowledgeBase() {
        CreateKnowledgeBaseRequest request = new CreateKnowledgeBaseRequest(
                "文档测试知识库",
                "用于文档服务单元测试"
        );

        return knowledgeBaseService.createKnowledgeBase(
                TEST_TENANT_ID,
                TEST_USER_ID,
                request
        );
    }
}