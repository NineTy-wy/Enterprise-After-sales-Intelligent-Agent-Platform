package com.agentplatform.backend.document.application;

import org.springframework.web.multipart.MultipartFile;

/**
 * 文件存储端口。
 *
 * <p>业务层只关心“保存文件并返回对象 key”，
 * 本地磁盘和 MinIO 由基础设施层分别实现。</p>
 */
public interface FileStorage {

    StoredFile store(
            String tenantId,
            String knowledgeBaseId,
            MultipartFile file
    );

    /**
     * 删除已经保存的对象，用于文档元数据写入失败时进行补偿清理。
     */
    void delete(String objectKey);

    /**
     * 存储结果只返回业务需要的稳定信息，不把底层客户端对象暴露出去。
     */
    record StoredFile(
            String objectKey,
            long size,
            String contentType
    ) {
    }
}
