package com.agentplatform.backend.document.api.dto;

import com.agentplatform.backend.document.domain.DocumentStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 更新文档处理状态请求。
 *
 * <p>后续 FastAPI Agent 服务完成文档解析、Chunk 切分、
 * Embedding 和向量入库后，可以通过接口回调该状态。</p>
 */
public record UpdateDocumentStatusRequest(

        /**
         * 目标状态。
         *
         * <p>当前支持 PROCESSING、INDEXED 和 FAILED。
         * 状态之间是否允许转换，由应用服务负责判断。</p>
         */
        @NotNull(message = "文档状态不能为空")
        DocumentStatus status,

        /**
         * 处理失败原因。
         *
         * <p>只有状态为 FAILED 时才有实际意义，
         * 但是否必填属于业务规则，放到 Service 层校验。</p>
         */
        @Size(max = 500, message = "失败原因不能超过 500 个字符")
        String failureReason
) {
}
