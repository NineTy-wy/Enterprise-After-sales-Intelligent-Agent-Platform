package com.agentplatform.backend.knowledge.application;

import com.agentplatform.backend.knowledge.api.dto.CreateKnowledgeBaseRequest;
import com.agentplatform.backend.knowledge.api.dto.KnowledgeBaseResponse;

import java.util.List;

/**
 * 知识库应用服务接口。
 *
 * <p>应用服务负责组织业务用例，例如创建知识库、查询知识库列表。
 * Controller 只负责接收 HTTP 请求，具体业务规则放在 Service 中。</p>
 */
public interface KnowledgeBaseService {

    /**
     * 创建知识库。
     *
     * @param tenantId 当前用户所属租户 ID
     * @param userId 当前操作用户 ID
     * @param request 创建知识库请求参数
     * @return 创建后的知识库信息
     */
    KnowledgeBaseResponse createKnowledgeBase(
            String tenantId,
            String userId,
            CreateKnowledgeBaseRequest request
    );

    /**
     * 查询当前租户下的知识库列表。
     *
     * @param tenantId 当前用户所属租户 ID
     * @return 知识库列表
     */
    List<KnowledgeBaseResponse> listKnowledgeBases(String tenantId);

    /**
     * 根据知识库 ID 查询详情。
     *
     * @param tenantId 当前用户所属租户 ID
     * @param knowledgeBaseId 知识库 ID
     * @return 知识库详情
     */
    KnowledgeBaseResponse getKnowledgeBase(
            String tenantId,
            String knowledgeBaseId
    );

    /**
     * 归档知识库。
     *
     * @param tenantId 当前用户所属租户 ID
     * @param userId 当前操作用户 ID
     * @param knowledgeBaseId 要归档的知识库 ID
     */
    void archiveKnowledgeBase(
            String tenantId,
            String userId,
            String knowledgeBaseId
    );
}