package com.helpdesk.dto;

import jakarta.validation.constraints.NotNull;

public record AssignRequest(@NotNull Long assigneeId) {
}
