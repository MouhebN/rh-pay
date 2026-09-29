package com.rhpaie.user.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Builder;

@Builder
public record UserStatusRequest(
        @NotNull(message = "Enabled status is required")
        Boolean enabled
) {
}
