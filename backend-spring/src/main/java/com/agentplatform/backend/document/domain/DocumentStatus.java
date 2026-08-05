package com.agentplatform.backend.document.domain;

/**
 * 文档处理状态。
 *
 * <p>文档从上传到可检索通常要经过异步处理流程，
 * 先用状态记录当前进度，便于前端展示和后端调度。</p>
 */
public enum DocumentStatus {

    /** 文档已上传，等待处理。 */
    UPLOADED,

    /** 文档正在解析、切分或入库。 */
    PROCESSING,

    /** 文档已处理完成，可用于检索。 */
    INDEXED,

    /** 文档处理失败，需要人工重试或排查。 */
    FAILED
}