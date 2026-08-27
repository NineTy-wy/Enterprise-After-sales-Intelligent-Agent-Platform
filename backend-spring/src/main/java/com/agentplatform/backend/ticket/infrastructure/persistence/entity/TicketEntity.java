package com.agentplatform.backend.ticket.infrastructure.persistence.entity;

import com.agentplatform.backend.ticket.domain.Ticket;
import com.agentplatform.backend.ticket.domain.TicketPriority;
import com.agentplatform.backend.ticket.domain.TicketStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.LocalDateTime;

/**
 * 工单数据库实体。
 */
@Entity
@Table(
        name = "support_tickets",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_ticket_tenant_no",
                columnNames = {"tenant_id", "ticket_no"}
        )
)
public class TicketEntity {

    @Id
    @Column(length = 36, nullable = false, updatable = false)
    private String id;

    @Column(name = "tenant_id", length = 64, nullable = false)
    private String tenantId;

    @Column(name = "ticket_no", length = 40, nullable = false)
    private String ticketNo;

    @Column(name = "customer_name", length = 100, nullable = false)
    private String customerName;

    @Column(name = "product_model", length = 100)
    private String productModel;

    @Column(name = "issue_description", length = 2000, nullable = false)
    private String issueDescription;

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private TicketPriority priority;

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private TicketStatus status;

    @Column(name = "assigned_to", length = 64)
    private String assignedTo;

    @Column(name = "created_by", length = 64, nullable = false)
    private String createdBy;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected TicketEntity() {
    }

    private TicketEntity(Ticket ticket) {
        id = ticket.getId();
        tenantId = ticket.getTenantId();
        ticketNo = ticket.getTicketNo();
        customerName = ticket.getCustomerName();
        productModel = ticket.getProductModel();
        issueDescription = ticket.getIssueDescription();
        priority = ticket.getPriority();
        status = ticket.getStatus();
        assignedTo = ticket.getAssignedTo();
        createdBy = ticket.getCreatedBy();
        createdAt = ticket.getCreatedAt();
        updatedAt = ticket.getUpdatedAt();
    }

    public static TicketEntity from(Ticket ticket) {
        return new TicketEntity(ticket);
    }

    public Ticket toDomain() {
        return Ticket.reconstitute(
                id,
                tenantId,
                ticketNo,
                customerName,
                productModel,
                issueDescription,
                priority,
                status,
                assignedTo,
                createdBy,
                createdAt,
                updatedAt
        );
    }
}
