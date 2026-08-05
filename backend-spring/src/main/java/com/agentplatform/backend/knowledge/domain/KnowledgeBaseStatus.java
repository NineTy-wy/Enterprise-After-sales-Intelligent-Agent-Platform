package com.agentplatform.backend.knowledge.domain;

/**
 * 知识库生命周期状态。
 *
 * <p>知识库不建议直接物理删除，因为后续可能需要保留文档、
 * 问答记录和审计信息。因此先通过状态控制是否可用。</p>
 */
public enum KnowledgeBaseStatus {

    /**
     * 正常使用中的知识库。
     *
     * <p>允许上传文档、执行检索和进行 Agent 问答。</p>
     */
    ACTIVE,

    /**
     * 已归档的知识库。
     *
     * <p>归档后默认不允许新增文档和执行普通检索，
     * 但历史数据仍然保留，便于审计和恢复。</p>
     */
    ARCHIVED
}
