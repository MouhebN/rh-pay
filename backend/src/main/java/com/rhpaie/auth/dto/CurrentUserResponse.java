package com.rhpaie.auth.dto;

import com.rhpaie.user.entity.Role;
import lombok.Builder;

@Builder
public record CurrentUserResponse(
        Long id,
        String username,
        String email,
        Role role
) {
}
