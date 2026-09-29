package com.rhpaie.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;

@Builder
public record LogoutRequest(
        @NotBlank(message = "Refresh token is required")
        String refreshToken
) {
}
