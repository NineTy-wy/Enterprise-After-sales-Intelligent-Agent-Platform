package com.agentplatform.backend.knowledge.application.impl;


import com.agentplatform.backend.common.error.BusinessException;
import com.agentplatform.backend.common.error.ErrorCode;
import com.agentplatform.backend.knowledge.api.dto.CreateKnowledgeBaseRequest;
import com.agentplatform.backend.knowledge.api.dto.KnowledgeBaseResponse;
import com.agentplatform.backend.knowledge.application.KnowledgeBaseService;
import com.agentplatform.backend.knowledge.domain.KnowledgeBaseStatus;
import com.agentplatform.backend.knowledge.infrastructure.repository.InMemoryKnowledgeBaseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * KnowledgeBaseServiceImpl 单元测试。
 *
 * <p>该测试不启动 Spring Boot，也不访问 HTTP 接口，
 * 只验证知识库业务服务本身是否符合预期。</p>
 */
class KnowledgeBaseServiceImplTest {

    /** 测试中固定使用的租户 ID，用于模拟多租户上下文。 */
    private static final String TEST_TENANT_ID = "tenant_test";

    /** 测试中固定使用的操作用户 ID。 */
    private static final String TEST_USER_ID = "user_test";

    /** 被测试的知识库服务。 */
    private KnowledgeBaseService knowledgeBaseService;

    /**
     * 每个测试方法执行前都会运行一次。
     *
     * <p>每次都创建新的内存仓储，避免不同测试之间互相污染数据。</p>
     */
    @BeforeEach
    void setUp() {
        knowledgeBaseService = new KnowledgeBaseServiceImpl(
                new InMemoryKnowledgeBaseRepository()
        );
    }

    /**
     * 验证创建知识库后，返回数据完整且状态为 ACTIVE。
     */
    @Test
    void createKnowledgeBase_shouldCreateActiveKnowledgeBase() {
        CreateKnowledgeBaseRequest request = new CreateKnowledgeBaseRequest(
                "售后测试知识库",
                "用于验证知识库创建功能"
        );

        KnowledgeBaseResponse response = knowledgeBaseService.createKnowledgeBase(
                TEST_TENANT_ID,
                TEST_USER_ID,
                request
        );

        assertNotNull(response.id());
        assertEquals("售后测试知识库", response.name());
        assertEquals("用于验证知识库创建功能", response.description());
        assertEquals(KnowledgeBaseStatus.ACTIVE, response.status());
        assertNotNull(response.createdAt());
        assertNotNull(response.updatedAt());
    }

    /**
     * 验证同一租户下不能创建同名知识库。
     *
     * <p>这个规则属于业务唯一性校验，应由 Service 层负责，
     * 不能只依赖前端判断。</p>
     */
    @Test
    void createKnowledgeBase_shouldRejectDuplicatedNameInSameTenant() {
        CreateKnowledgeBaseRequest request = new CreateKnowledgeBaseRequest(
                "重复名称知识库",
                "第一次创建"
        );

        knowledgeBaseService.createKnowledgeBase(
                TEST_TENANT_ID,
                TEST_USER_ID,
                request
        );

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> knowledgeBaseService.createKnowledgeBase(
                        TEST_TENANT_ID,
                        TEST_USER_ID,
                        request
                )
        );

        assertEquals(ErrorCode.INVALID_REQUEST, exception.getErrorCode());
    }

    /**
     * 验证已经归档的知识库不能重复归档。
     *
     * <p>重复归档属于资源状态冲突，应返回明确的业务错误码，
     * 而不是被当作系统内部异常。</p>
     */
    @Test
    void archiveKnowledgeBase_shouldRejectAlreadyArchivedKnowledgeBase() {
        CreateKnowledgeBaseRequest request = new CreateKnowledgeBaseRequest(
                "归档状态测试知识库",
                "用于验证重复归档规则"
        );

        KnowledgeBaseResponse createdKnowledgeBase =
                knowledgeBaseService.createKnowledgeBase(
                        TEST_TENANT_ID,
                        TEST_USER_ID,
                        request
                );

        // 第一次归档应正常完成。
        knowledgeBaseService.archiveKnowledgeBase(
                TEST_TENANT_ID,
                TEST_USER_ID,
                createdKnowledgeBase.id()
        );

        // 第二次归档同一个知识库，应抛出状态冲突业务异常。
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> knowledgeBaseService.archiveKnowledgeBase(
                        TEST_TENANT_ID,
                        TEST_USER_ID,
                        createdKnowledgeBase.id()
                )
        );

        assertEquals(
                ErrorCode.RESOURCE_STATE_CONFLICT,
                exception.getErrorCode()
        );
    }

    /**
     * 验证跨租户不能通过 ID 查询知识库。
     *
     * <p>即使调用方知道另一个租户的知识库 ID，
     * 也不能拿到该知识库详情，应统一返回资源不存在。</p>
     */
    @Test
    void getKnowledgeBase_shouldHideKnowledgeBaseFromOtherTenant() {
        CreateKnowledgeBaseRequest request = new CreateKnowledgeBaseRequest(
                "租户隔离测试知识库",
                "用于验证跨租户访问限制"
        );

        KnowledgeBaseResponse createdKnowledgeBase =
                knowledgeBaseService.createKnowledgeBase(
                        TEST_TENANT_ID,
                        TEST_USER_ID,
                        request
                );

        String otherTenantId = "tenant_other";

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> knowledgeBaseService.getKnowledgeBase(
                        otherTenantId,
                        createdKnowledgeBase.id()
                )
        );

        assertEquals(ErrorCode.RESOURCE_NOT_FOUND, exception.getErrorCode());
    }
}
