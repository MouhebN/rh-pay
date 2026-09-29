package com.rhpaie.user.dto;

import com.rhpaie.user.entity.Role;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record UserResponse(
        Long id,
        String username,
        String email,
        Role role,
        boolean enabled,
        boolean accountLocked,
        int failedLoginAttempts,
        LocalDateTime lastLoginAt,
        LocalDateTime passwordChangedAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
