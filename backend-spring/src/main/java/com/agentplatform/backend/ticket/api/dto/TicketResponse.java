package com.agentplatform.backend.ticket.api.dto;

import com.agentplatform.backend.ticket.domain.Ticket;
import com.agentplatform.backend.ticket.domain.TicketPriority;
import com.agentplatform.backend.ticket.domain.TicketStatus;

import java.time.LocalDateTime;

/**
 * 工单响应。
 */
public record TicketResponse(
        String id,
        String ticketNo,
        String customerName,
        String productModel,
        String issueDescription,
        TicketPriority priority,
        TicketStatus status,
        String assignedTo,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public static TicketResponse from(Ticket ticket) {
        return new TicketResponse(
                ticket.getId(),
                ticket.getTicketNo(),
                ticket.getCustomerName(),
                ticket.getProductModel(),
                ticket.getIssueDescription(),
                ticket.getPriority(),
                ticket.getStatus(),
                ticket.getAssignedTo(),
                ticket.getCreatedAt(),
                ticket.getUpdatedAt()
        );
    }
}
