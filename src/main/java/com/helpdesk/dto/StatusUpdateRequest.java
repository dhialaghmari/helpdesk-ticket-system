package com.helpdesk.dto;

import com.helpdesk.entity.TicketStatus;
import jakarta.validation.constraints.NotNull;

public record StatusUpdateRequest(@NotNull TicketStatus status) {
}
