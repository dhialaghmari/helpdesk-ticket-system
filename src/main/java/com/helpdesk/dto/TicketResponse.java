package com.helpdesk.dto;

import com.helpdesk.entity.Category;
import com.helpdesk.entity.Priority;
import com.helpdesk.entity.Ticket;
import com.helpdesk.entity.TicketStatus;

import java.time.LocalDateTime;

public record TicketResponse(
        Long id,
        String title,
        String description,
        TicketStatus status,
        Priority priority,
        Category category,
        String createdBy,
        String assignedTo,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static TicketResponse from(Ticket t) {
        return new TicketResponse(
                t.getId(),
                t.getTitle(),
                t.getDescription(),
                t.getStatus(),
                t.getPriority(),
                t.getCategory(),
                t.getCreatedBy().getUsername(),
                t.getAssignedTo() == null ? null : t.getAssignedTo().getUsername(),
                t.getCreatedAt(),
                t.getUpdatedAt());
    }
}
