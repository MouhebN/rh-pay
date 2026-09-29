package com.rhpaie.auth.mapper;

import com.rhpaie.auth.dto.CurrentUserResponse;
import com.rhpaie.auth.dto.LoginResponse;
import com.rhpaie.user.entity.UserAccount;
import org.springframework.stereotype.Component;

@Component
public class AuthMapper {

    public LoginResponse toLoginResponse(
            UserAccount userAccount,
            String accessToken,
            String refreshToken,
            long expiresIn
    ) {
        return LoginResponse.builder()
                .token(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(expiresIn)
                .userId(userAccount.getId())
                .username(userAccount.getUsername())
                .email(userAccount.getEmail())
                .role(userAccount.getRole())
                .build();
    }

    public CurrentUserResponse toCurrentUserResponse(UserAccount userAccount) {
        return CurrentUserResponse.builder()
                .id(userAccount.getId())
                .username(userAccount.getUsername())
                .email(userAccount.getEmail())
                .role(userAccount.getRole())
                .build();
    }
}
