package com.agentplatform.backend.ticket.infrastructure.persistence.repository;

import com.agentplatform.backend.ticket.infrastructure.persistence.entity.TicketEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Spring Data 工单表访问接口。
 */
public interface SpringDataTicketRepository
        extends JpaRepository<TicketEntity, String> {

    List<TicketEntity> findAllByTenantIdOrderByUpdatedAtDesc(String tenantId);
}
