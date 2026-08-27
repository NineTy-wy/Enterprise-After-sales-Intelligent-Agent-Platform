package com.agentplatform.backend.ticket.infrastructure.repository;

import com.agentplatform.backend.ticket.domain.Ticket;
import com.agentplatform.backend.ticket.domain.TicketRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 本地工单仓储。
 */
@Repository
@Profile("local")
public class InMemoryTicketRepository implements TicketRepository {

    private final ConcurrentMap<String, Ticket> storage = new ConcurrentHashMap<>();

    @Override
    public Ticket save(Ticket ticket) {
        storage.put(ticket.getId(), ticket);
        return ticket;
    }

    @Override
    public Optional<Ticket> findById(String id) {
        return Optional.ofNullable(storage.get(id));
    }

    @Override
    public List<Ticket> findByTenantId(String tenantId) {
        return storage.values().stream()
                .filter(ticket -> tenantId.equals(ticket.getTenantId()))
                .sorted((left, right) -> right.getUpdatedAt()
                        .compareTo(left.getUpdatedAt()))
                .toList();
    }
}
