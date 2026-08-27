package com.agentplatform.backend.ticket.infrastructure.persistence.repository;

import com.agentplatform.backend.ticket.domain.Ticket;
import com.agentplatform.backend.ticket.domain.TicketRepository;
import com.agentplatform.backend.ticket.infrastructure.persistence.entity.TicketEntity;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * PostgreSQL 工单仓储适配器。
 */
@Repository
@Profile("postgres")
public class PostgresTicketRepository implements TicketRepository {

    private final SpringDataTicketRepository repository;

    public PostgresTicketRepository(SpringDataTicketRepository repository) {
        this.repository = repository;
    }

    @Override
    public Ticket save(Ticket ticket) {
        return repository.save(TicketEntity.from(ticket)).toDomain();
    }

    @Override
    public Optional<Ticket> findById(String id) {
        return repository.findById(id).map(TicketEntity::toDomain);
    }

    @Override
    public List<Ticket> findByTenantId(String tenantId) {
        return repository.findAllByTenantIdOrderByUpdatedAtDesc(tenantId)
                .stream()
                .map(TicketEntity::toDomain)
                .toList();
    }
}
