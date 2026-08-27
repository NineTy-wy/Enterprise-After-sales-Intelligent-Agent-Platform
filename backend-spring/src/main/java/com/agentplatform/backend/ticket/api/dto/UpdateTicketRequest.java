package com.agentplatform.backend.ticket.api.dto;

import com.agentplatform.backend.ticket.domain.TicketStatus;
import jakarta.validation.constraints.Size;

/**
 * 工单更新请求。
 */
public record UpdateTicketRequest(
        TicketStatus status,
        @Size(max = 64, message = "处理人 ID 不能超过 64 个字符")
        String assignedTo
) {
}
