package com.agentplatform.backend.document.domain;

import java.util.List;
import java.util.Optional;

/**
 * 文档仓储接口。
 *
 * <p>应用服务只依赖该接口，不依赖具体的内存、PostgreSQL
 * 或其他存储实现，便于后续替换基础设施。</p>
 */
public interface DocumentRepository {

    /**
     * 保存文档。
     *
     * <p>新文档会新增记录；已有文档会更新状态或失败原因等字段。</p>
     */
    Document save(Document document);

    /**
     * 根据文档 ID 查询文档。
     */
    Optional<Document> findById(String id);

    /**
     * 查询某个知识库下的全部文档。
     *
     * <p>后续可以在数据库实现中增加分页、排序和状态过滤。</p>
     */
    List<Document> findByKnowledgeBaseId(String knowledgeBaseId);
}
