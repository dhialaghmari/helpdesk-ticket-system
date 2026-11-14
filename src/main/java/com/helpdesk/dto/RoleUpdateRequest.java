package com.helpdesk.dto;

import com.helpdesk.entity.Role;
import jakarta.validation.constraints.NotNull;

public record RoleUpdateRequest(@NotNull Role role) {
}
