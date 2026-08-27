package com.agentplatform.backend.ticket.api.dto;

import com.agentplatform.backend.ticket.domain.TicketPriority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 创建工单请求。
 */
public record CreateTicketRequest(
        @NotBlank(message = "客户姓名不能为空")
        @Size(max = 100, message = "客户姓名不能超过 100 个字符")
        String customerName,
        @Size(max = 100, message = "产品型号不能超过 100 个字符")
        String productModel,
        @NotBlank(message = "问题描述不能为空")
        @Size(max = 2000, message = "问题描述不能超过 2000 个字符")
        String issueDescription,
        TicketPriority priority
) {
}
