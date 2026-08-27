package com.agentplatform.backend.ticket.domain;

import java.util.List;
import java.util.Optional;

/**
 * 工单仓储接口。
 */
public interface TicketRepository {

    Ticket save(Ticket ticket);

    Optional<Ticket> findById(String id);

    List<Ticket> findByTenantId(String tenantId);
}
