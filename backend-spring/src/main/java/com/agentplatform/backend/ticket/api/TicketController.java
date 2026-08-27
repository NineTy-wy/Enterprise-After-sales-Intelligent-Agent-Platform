package com.agentplatform.backend.ticket.api;

import com.agentplatform.backend.audit.application.AuditService;
import com.agentplatform.backend.common.api.ApiResponse;
import com.agentplatform.backend.common.security.CurrentUser;
import com.agentplatform.backend.common.security.CurrentUserProvider;
import com.agentplatform.backend.ticket.api.dto.CreateTicketRequest;
import com.agentplatform.backend.ticket.api.dto.TicketResponse;
import com.agentplatform.backend.ticket.api.dto.UpdateTicketRequest;
import com.agentplatform.backend.ticket.application.TicketService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 售后工单接口。
 */
@RestController
@RequestMapping("/api/tickets")
public class TicketController {

    private final TicketService ticketService;
    private final CurrentUserProvider currentUserProvider;
    private final AuditService auditService;

    public TicketController(
            TicketService ticketService,
            CurrentUserProvider currentUserProvider,
            AuditService auditService
    ) {
        this.ticketService = ticketService;
        this.currentUserProvider = currentUserProvider;
        this.auditService = auditService;
    }

    @PostMapping
    public ApiResponse<TicketResponse> createTicket(
            @Valid @RequestBody CreateTicketRequest request
    ) {
        CurrentUser user = currentUserProvider.currentUser();
        TicketResponse response = ticketService.createTicket(
                user.tenantId(),
                user.userId(),
                request
        );
        auditService.record(
                user.tenantId(),
                user.userId(),
                "CREATE",
                "TICKET",
                response.id(),
                "{\"priority\":\"" + response.priority().name() + "\"}"
        );
        return ApiResponse.success(response);
    }

    @GetMapping
    public ApiResponse<List<TicketResponse>> listTickets() {
        return ApiResponse.success(ticketService.listTickets(
                currentUserProvider.currentUser().tenantId()
        ));
    }

    @GetMapping("/{ticketId}")
    public ApiResponse<TicketResponse> getTicket(
            @PathVariable String ticketId
    ) {
        CurrentUser user = currentUserProvider.currentUser();
        return ApiResponse.success(ticketService.getTicket(
                user.tenantId(),
                ticketId
        ));
    }

    @PatchMapping("/{ticketId}")
    public ApiResponse<TicketResponse> updateTicket(
            @PathVariable String ticketId,
            @Valid @RequestBody UpdateTicketRequest request
    ) {
        CurrentUser user = currentUserProvider.currentUser();
        TicketResponse response = ticketService.updateTicket(
                user.tenantId(),
                ticketId,
                request
        );
        auditService.record(
                user.tenantId(),
                user.userId(),
                "UPDATE",
                "TICKET",
                ticketId,
                "{\"status\":\"" + (request.status() == null
                        ? "" : request.status().name()) + "\"}"
        );
        return ApiResponse.success(response);
    }
}
