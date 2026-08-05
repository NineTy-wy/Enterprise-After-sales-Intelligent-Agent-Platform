package com.agentplatform.backend.knowledge.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 创建知识库请求。
 *
 * <p>DTO 只负责表达接口入参，不承载业务逻辑。
 * 参数校验放在 DTO 上，可以让错误尽早在 Controller 层被拦截。</p>
 */
public record CreateKnowledgeBaseRequest(

        /**
         * 知识库名称。
         *
         * <p>不能为空，长度限制在 2 到 50 个字符，
         * 避免出现空名称或超长名称影响前端展示。</p>
         */
        @NotBlank(message = "知识库名称不能为空")
        @Size(min = 2, max = 50, message = "知识库名称长度必须在 2 到 50 个字符之间")
        String name,

        /**
         * 知识库描述。
         *
         * <p>描述不是必填，但限制最大长度，避免写入过大的无结构文本。</p>
         */
        @Size(max = 200, message = "知识库描述不能超过 200 个字符")
        String description
) {
}
