package com.agentplatform.backend.knowledge.api;

import com.agentplatform.backend.common.api.ApiResponse;
import com.agentplatform.backend.knowledge.api.dto.CreateKnowledgeBaseRequest;
import com.agentplatform.backend.knowledge.api.dto.KnowledgeBaseResponse;
import com.agentplatform.backend.knowledge.application.KnowledgeBaseService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

/**
 * 知识库接口控制器。
 *
 * <p>Controller 只负责 HTTP 协议相关工作：
 * 接收请求、触发参数校验、调用应用服务、包装统一响应。</p>
 */
@RestController
@RequestMapping("/api/knowledge-bases")
public class KnowledgeBaseController {

    /** 当前阶段使用固定租户 ID，后续接入登录后从 JWT 中获取。 */
    private static final String DEMO_TENANT_ID = "tenant_demo";

    /** 当前阶段使用固定用户 ID，后续接入登录后从认证上下文中获取。 */
    private static final String DEMO_USER_ID = "user_demo";

    /** 知识库应用服务，承载创建和查询等业务用例。 */
    private final KnowledgeBaseService knowledgeBaseService;

    /**
     * 使用构造方法注入 Service，保持依赖清晰且便于测试。
     */
    public KnowledgeBaseController(KnowledgeBaseService knowledgeBaseService) {
        this.knowledgeBaseService = knowledgeBaseService;
    }

    /**
     * 创建知识库。
     *
     * <p>@Valid 会触发 CreateKnowledgeBaseRequest 上的参数校验规则。</p>
     */
    @PostMapping
    public ApiResponse<KnowledgeBaseResponse> createKnowledgeBase(
            @Valid @RequestBody CreateKnowledgeBaseRequest request
    ) {
        KnowledgeBaseResponse response = knowledgeBaseService.createKnowledgeBase(
                DEMO_TENANT_ID,
                DEMO_USER_ID,
                request
        );
        return ApiResponse.success(response);
    }

    /**
     * 查询当前租户下的知识库列表。
     */
    @GetMapping
    public ApiResponse<List<KnowledgeBaseResponse>> listKnowledgeBases() {
        List<KnowledgeBaseResponse> responses =
                knowledgeBaseService.listKnowledgeBases(DEMO_TENANT_ID);
        return ApiResponse.success(responses);
    }

    /**
     * 根据 ID 查询知识库详情。
     *
     * <p>当前租户 ID 暂时使用演示值；
     * 后续接入 JWT 后会从当前登录用户上下文中获取。</p>
     */
    @GetMapping("/{knowledgeBaseId}")
    public ApiResponse<KnowledgeBaseResponse> getKnowledgeBase(
            @PathVariable String knowledgeBaseId
    ) {
        KnowledgeBaseResponse response = knowledgeBaseService.getKnowledgeBase(
                DEMO_TENANT_ID,
                knowledgeBaseId
        );
        return ApiResponse.success(response);
    }

    /**
     * 归档知识库。
     *
     * <p>这里使用 POST 表示触发一个业务动作。
     * 归档后知识库不会被物理删除，而是状态变为 ARCHIVED。</p>
     */
    @PostMapping("/{knowledgeBaseId}/archive")
    public ApiResponse<Void> archiveKnowledgeBase(
            @PathVariable String knowledgeBaseId
    ) {
        knowledgeBaseService.archiveKnowledgeBase(
                DEMO_TENANT_ID,
                DEMO_USER_ID,
                knowledgeBaseId
        );
        return ApiResponse.success();
    }
}
