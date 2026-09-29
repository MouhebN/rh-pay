package com.rhpaie.user.mapper;

import com.rhpaie.user.dto.UserResponse;
import com.rhpaie.user.entity.UserAccount;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    public UserResponse toResponse(UserAccount userAccount) {
        return UserResponse.builder()
                .id(userAccount.getId())
                .username(userAccount.getUsername())
                .email(userAccount.getEmail())
                .role(userAccount.getRole())
                .enabled(userAccount.isEnabled())
                .accountLocked(userAccount.isAccountLocked())
                .failedLoginAttempts(userAccount.getFailedLoginAttempts())
                .lastLoginAt(userAccount.getLastLoginAt())
                .passwordChangedAt(userAccount.getPasswordChangedAt())
                .createdAt(userAccount.getCreatedAt())
                .updatedAt(userAccount.getUpdatedAt())
                .build();
    }
}
