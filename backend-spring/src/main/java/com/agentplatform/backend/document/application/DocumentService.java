package com.agentplatform.backend.document.application;

import com.agentplatform.backend.document.api.dto.CreateDocumentRequest;
import com.agentplatform.backend.document.api.dto.DocumentResponse;
import com.agentplatform.backend.document.api.dto.UpdateDocumentStatusRequest;

import java.util.List;

/**
 * 文档应用服务接口。
 *
 * <p>负责组织文档相关业务用例，例如登记上传文档、
 * 查询知识库下的文档列表。</p>
 */
public interface DocumentService {

    /**
     * 创建文档记录。
     *
     * <p>创建前需要确认目标知识库存在、属于当前租户，
     * 且处于允许新增文档的状态。</p>
     *
     * @param tenantId 当前用户所属租户 ID
     * @param userId 当前操作用户 ID
     * @param request 文档元数据请求
     * @return 创建后的文档信息
     */
    DocumentResponse createDocument(
            String tenantId,
            String userId,
            CreateDocumentRequest request
    );

    /**
     * 查询某个知识库下的文档列表。
     *
     * <p>查询前需要确认该知识库属于当前租户，
     * 防止通过知识库 ID 跨租户读取文档信息。</p>
     *
     * @param tenantId 当前用户所属租户 ID
     * @param knowledgeBaseId 知识库 ID
     * @return 文档列表
     */
    List<DocumentResponse> listDocuments(
            String tenantId,
            String knowledgeBaseId
    );

    /**
     * 更新文档处理状态。
     *
     * <p>后续由 FastAPI Agent 服务在文档解析、Chunk 切分、
     * Embedding 和向量入库完成后调用。</p>
     *
     * @param tenantId 当前租户 ID
     * @param operatorId 执行状态更新的操作者或服务标识
     * @param documentId 要更新的文档 ID
     * @param request 目标状态和失败原因
     * @return 更新后的文档信息
     */
    DocumentResponse updateDocumentStatus(
            String tenantId,
            String operatorId,
            String documentId,
            UpdateDocumentStatusRequest request
    );
}
