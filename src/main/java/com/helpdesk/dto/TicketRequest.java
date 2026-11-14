package com.helpdesk.dto;

import com.helpdesk.entity.Category;
import com.helpdesk.entity.Priority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Payload used to create or update a ticket. Priority defaults to MEDIUM and category to OTHER. */
public record TicketRequest(
        @NotBlank
        @Size(max = 150)
        String title,

        @NotBlank
        @Size(max = 5000)
        String description,

        Priority priority,

        Category category
) {
}
