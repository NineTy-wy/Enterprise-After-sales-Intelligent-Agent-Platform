package com.agentplatform.backend.knowledge.domain;

import java.util.List;
import java.util.Optional;

/**
 * 知识库仓储接口。
 *
 * <p>Repository 负责屏蔽底层数据来源。业务层只关心“保存知识库”“查询知识库”，
 * 不关心数据到底来自内存、PostgreSQL 还是其他存储。</p>
 */
public interface KnowledgeBaseRepository {

    /**
     * 保存知识库。
     *
     * <p>对于新知识库是新增；对于已有知识库是更新。</p>
     */
    KnowledgeBase save(KnowledgeBase knowledgeBase);

    /**
     * 根据 ID 查询知识库。
     */
    Optional<KnowledgeBase> findById(String id);

    /**
     * 查询某个租户下的全部知识库。
     */
    List<KnowledgeBase> findByTenantId(String tenantId);

    /**
     * 判断某个租户下是否已经存在同名知识库。
     *
     * <p>用于创建知识库时做业务唯一性校验。</p>
     */
    boolean existsByTenantIdAndName(String tenantId, String name);
}
