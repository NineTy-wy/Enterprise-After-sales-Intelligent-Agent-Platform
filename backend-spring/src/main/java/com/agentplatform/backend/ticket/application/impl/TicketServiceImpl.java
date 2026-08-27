package com.agentplatform.backend.ticket.application.impl;

import com.agentplatform.backend.common.error.BusinessException;
import com.agentplatform.backend.common.error.ErrorCode;
import com.agentplatform.backend.ticket.api.dto.CreateTicketRequest;
import com.agentplatform.backend.ticket.api.dto.TicketResponse;
import com.agentplatform.backend.ticket.api.dto.UpdateTicketRequest;
import com.agentplatform.backend.ticket.application.TicketService;
import com.agentplatform.backend.ticket.domain.Ticket;
import com.agentplatform.backend.ticket.domain.TicketRepository;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 工单应用服务实现。
 */
@Service
public class TicketServiceImpl implements TicketService {

    private final TicketRepository ticketRepository;

    public TicketServiceImpl(TicketRepository ticketRepository) {
        this.ticketRepository = ticketRepository;
    }

    @Override
    public TicketResponse createTicket(
            String tenantId,
            String userId,
            CreateTicketRequest request
    ) {
        Ticket ticket = Ticket.create(
                tenantId,
                request.customerName(),
                request.productModel(),
                request.issueDescription(),
                request.priority(),
                userId
        );
        return TicketResponse.from(ticketRepository.save(ticket));
    }

    @Override
    public TicketResponse getTicket(String tenantId, String ticketId) {
        return TicketResponse.from(findOwnedTicket(tenantId, ticketId));
    }

    @Override
    public List<TicketResponse> listTickets(String tenantId) {
        return ticketRepository.findByTenantId(tenantId)
                .stream()
                .map(TicketResponse::from)
                .toList();
    }

    @Override
    public TicketResponse updateTicket(
            String tenantId,
            String ticketId,
            UpdateTicketRequest request
    ) {
        Ticket ticket = findOwnedTicket(tenantId, ticketId);
        try {
            if (request.status() != null) {
                ticket.updateStatus(request.status());
            }
            if (request.assignedTo() != null
                    && !request.assignedTo().isBlank()) {
                ticket.assignTo(request.assignedTo());
            }
        } catch (IllegalStateException exception) {
            throw new BusinessException(
                    ErrorCode.RESOURCE_STATE_CONFLICT,
                    exception.getMessage()
            );
        }
        return TicketResponse.from(ticketRepository.save(ticket));
    }

    private Ticket findOwnedTicket(String tenantId, String ticketId) {
        return ticketRepository.findById(ticketId)
                .filter(ticket -> tenantId.equals(ticket.getTenantId()))
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.RESOURCE_NOT_FOUND,
                        "工单不存在"
                ));
    }
}
