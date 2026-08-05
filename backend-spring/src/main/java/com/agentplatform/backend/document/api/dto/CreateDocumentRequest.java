package com.agentplatform.backend.document.api.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 创建文档记录请求。
 *
 * <p>第一阶段先登记文档元数据，不直接接收文件流。
 * 后续接入 MultipartFile、MinIO 和异步解析任务后，
 * 该请求会演进为真实上传接口的一部分。</p>
 */
public record CreateDocumentRequest(

        /** 所属知识库 ID。 */
        @NotBlank(message = "知识库 ID 不能为空")
        String knowledgeBaseId,

        /** 原始文件名。 */
        @NotBlank(message = "文件名不能为空")
        @Size(max = 200, message = "文件名不能超过 200 个字符")
        String fileName,

        /** 文件类型，例如 pdf、docx、xlsx。 */
        @NotBlank(message = "文件类型不能为空")
        @Size(max = 20, message = "文件类型不能超过 20 个字符")
        String fileType,

        /** 文件大小，单位字节。 */
        @Min(value = 1, message = "文件大小必须大于 0")
        long fileSize,

        /** 文件存储路径，后续可对应 MinIO object key。 */
        @NotBlank(message = "文件存储路径不能为空")
        @Size(max = 500, message = "文件存储路径不能超过 500 个字符")
        String storagePath
) {
}
