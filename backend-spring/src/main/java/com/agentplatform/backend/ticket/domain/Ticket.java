package com.agentplatform.backend.ticket.domain;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 售后工单领域对象。
 */
public class Ticket {

    private String id;
    private String tenantId;
    private String ticketNo;
    private String customerName;
    private String productModel;
    private String issueDescription;
    private TicketPriority priority;
    private TicketStatus status;
    private String assignedTo;
    private String createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Ticket() {
    }

    public static Ticket create(
            String tenantId,
            String customerName,
            String productModel,
            String issueDescription,
            TicketPriority priority,
            String createdBy
    ) {
        LocalDateTime now = LocalDateTime.now();
        Ticket ticket = new Ticket();
        ticket.id = UUID.randomUUID().toString();
        ticket.tenantId = tenantId;
        ticket.ticketNo = "TK-" + now.toLocalDate().toString().replace("-", "")
                + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        ticket.customerName = customerName;
        ticket.productModel = productModel;
        ticket.issueDescription = issueDescription;
        ticket.priority = priority == null ? TicketPriority.MEDIUM : priority;
        ticket.status = TicketStatus.OPEN;
        ticket.createdBy = createdBy;
        ticket.createdAt = now;
        ticket.updatedAt = now;
        return ticket;
    }

    public static Ticket reconstitute(
            String id,
            String tenantId,
            String ticketNo,
            String customerName,
            String productModel,
            String issueDescription,
            TicketPriority priority,
            TicketStatus status,
            String assignedTo,
            String createdBy,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        Ticket ticket = new Ticket();
        ticket.id = id;
        ticket.tenantId = tenantId;
        ticket.ticketNo = ticketNo;
        ticket.customerName = customerName;
        ticket.productModel = productModel;
        ticket.issueDescription = issueDescription;
        ticket.priority = priority;
        ticket.status = status;
        ticket.assignedTo = assignedTo;
        ticket.createdBy = createdBy;
        ticket.createdAt = createdAt;
        ticket.updatedAt = updatedAt;
        return ticket;
    }

    public void updateStatus(TicketStatus targetStatus) {
        if (targetStatus == null) {
            throw new IllegalStateException("工单状态不能为空");
        }
        if (status == TicketStatus.CLOSED && targetStatus != TicketStatus.CLOSED) {
            throw new IllegalStateException("已关闭工单不能重新打开");
        }
        if (status == TicketStatus.OPEN && targetStatus == TicketStatus.RESOLVED) {
            throw new IllegalStateException("未处理工单不能直接标记为已解决");
        }
        status = targetStatus;
        updatedAt = LocalDateTime.now();
    }

    public void assignTo(String userId) {
        assignedTo = userId;
        updatedAt = LocalDateTime.now();
    }

    public String getId() {
        return id;
    }

    public String getTenantId() {
        return tenantId;
    }

    public String getTicketNo() {
        return ticketNo;
    }

    public String getCustomerName() {
        return customerName;
    }

    public String getProductModel() {
        return productModel;
    }

    public String getIssueDescription() {
        return issueDescription;
    }

    public TicketPriority getPriority() {
        return priority;
    }

    public TicketStatus getStatus() {
        return status;
    }

    public String getAssignedTo() {
        return assignedTo;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
