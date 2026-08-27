package com.agentplatform.backend.ticket.application;

import com.agentplatform.backend.ticket.api.dto.CreateTicketRequest;
import com.agentplatform.backend.ticket.api.dto.TicketResponse;
import com.agentplatform.backend.ticket.api.dto.UpdateTicketRequest;

import java.util.List;

/**
 * 工单应用服务。
 */
public interface TicketService {

    TicketResponse createTicket(
            String tenantId,
            String userId,
            CreateTicketRequest request
    );

    TicketResponse getTicket(String tenantId, String ticketId);

    List<TicketResponse> listTickets(String tenantId);

    TicketResponse updateTicket(
            String tenantId,
            String ticketId,
            UpdateTicketRequest request
    );
}
