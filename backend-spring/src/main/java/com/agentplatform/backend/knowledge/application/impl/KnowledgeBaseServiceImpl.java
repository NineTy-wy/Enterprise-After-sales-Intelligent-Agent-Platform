package com.agentplatform.backend.knowledge.application.impl;

import com.agentplatform.backend.common.error.BusinessException;
import com.agentplatform.backend.common.error.ErrorCode;
import com.agentplatform.backend.knowledge.api.dto.CreateKnowledgeBaseRequest;
import com.agentplatform.backend.knowledge.api.dto.KnowledgeBaseResponse;
import com.agentplatform.backend.knowledge.application.KnowledgeBaseService;
import com.agentplatform.backend.knowledge.domain.KnowledgeBase;
import com.agentplatform.backend.knowledge.domain.KnowledgeBaseRepository;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 知识库应用服务实现。
 *
 * <p>这里承载创建知识库、查询知识库等业务用例，
 * 是 Controller 与领域模型、仓储之间的协调层。</p>
 */
@Service
public class KnowledgeBaseServiceImpl implements KnowledgeBaseService {

    /** 知识库仓储接口，当前注入的是内存实现，后续可替换为 PostgreSQL 实现。 */
    private final KnowledgeBaseRepository knowledgeBaseRepository;

    /**
     * 使用构造方法注入依赖。
     *
     * <p>相比字段注入，构造方法注入可以让依赖更明确，
     * 也更方便单元测试。</p>
     */
    public KnowledgeBaseServiceImpl(KnowledgeBaseRepository knowledgeBaseRepository) {
        this.knowledgeBaseRepository = knowledgeBaseRepository;
    }

    @Override
    public KnowledgeBaseResponse createKnowledgeBase(
            String tenantId,
            String userId,
            CreateKnowledgeBaseRequest request
    ) {
        if (knowledgeBaseRepository.existsByTenantIdAndName(tenantId, request.name())) {
            throw new BusinessException(
                    ErrorCode.INVALID_REQUEST,
                    "当前租户下已存在同名知识库"
            );
        }

        KnowledgeBase knowledgeBase = KnowledgeBase.create(
                tenantId,
                request.name(),
                request.description(),
                userId
        );

        KnowledgeBase savedKnowledgeBase = knowledgeBaseRepository.save(knowledgeBase);
        return KnowledgeBaseResponse.from(savedKnowledgeBase);
    }

    @Override
    public List<KnowledgeBaseResponse> listKnowledgeBases(String tenantId) {
        return knowledgeBaseRepository.findByTenantId(tenantId)
                .stream()
                .map(KnowledgeBaseResponse::from)
                .toList();
    }

    @Override
    public KnowledgeBaseResponse getKnowledgeBase(
            String tenantId,
            String knowledgeBaseId
    ) {
        KnowledgeBase knowledgeBase = knowledgeBaseRepository.findById(knowledgeBaseId)
                .filter(item -> tenantId.equals(item.getTenantId()))
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.RESOURCE_NOT_FOUND,
                        "知识库不存在"
                ));

        return KnowledgeBaseResponse.from(knowledgeBase);
    }

    @Override
    public void archiveKnowledgeBase(
            String tenantId,
            String userId,
            String knowledgeBaseId
    ) {
        KnowledgeBase knowledgeBase = knowledgeBaseRepository.findById(knowledgeBaseId)
                .filter(item -> tenantId.equals(item.getTenantId()))
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.RESOURCE_NOT_FOUND,
                        "知识库不存在"
                ));

        /**
         * 状态冲突属于可预期的业务错误，应返回明确的 409 错误，
         * 而不是让领域对象抛出的 IllegalStateException 被当成系统异常。
         */
        if (!knowledgeBase.isActive()) {
            throw new BusinessException(
                    ErrorCode.RESOURCE_STATE_CONFLICT,
                    "知识库已归档，不能重复归档"
            );
        }

        knowledgeBase.archive();
        knowledgeBaseRepository.save(knowledgeBase);
    }
}
