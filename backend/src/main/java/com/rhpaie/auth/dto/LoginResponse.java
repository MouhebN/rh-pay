package com.rhpaie.auth.dto;

import com.rhpaie.user.entity.Role;
import lombok.Builder;

@Builder
public record LoginResponse(
        String token,
        String refreshToken,
        String tokenType,
        long expiresIn,
        Long userId,
        String username,
        String email,
        Role role
) {
}
