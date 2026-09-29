package com.rhpaie.auth.controller;

import com.rhpaie.auth.dto.CurrentUserResponse;
import com.rhpaie.auth.dto.LoginRequest;
import com.rhpaie.auth.dto.LoginResponse;
import com.rhpaie.auth.dto.LogoutRequest;
import com.rhpaie.auth.dto.RefreshTokenRequest;
import com.rhpaie.auth.dto.RegisterRequest;
import com.rhpaie.auth.service.AuthService;
import com.rhpaie.common.response.ApiResponse;
import com.rhpaie.common.security.SecurityConstants;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;

@RestController
@RequestMapping(SecurityConstants.AUTH_BASE_PATH)
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @GetMapping("/health")
    public ApiResponse<String> health() {
        return ApiResponse.success("Authentication module is available", authService.health());
    }

    @PostMapping("/register")
    public ApiResponse<CurrentUserResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ApiResponse.success("User registered successfully", authService.register(request));
    }

    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest
    ) {
        return ApiResponse.success("Login successful", authService.login(request, httpRequest));
    }

    @PostMapping("/refresh")
    public ApiResponse<LoginResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return ApiResponse.success("Token refreshed successfully", authService.refresh(request));
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(@Valid @RequestBody LogoutRequest request) {
        authService.logout(request.refreshToken());
        return ApiResponse.success("Logout successful");
    }

    @GetMapping("/me")
    public ApiResponse<CurrentUserResponse> currentUser(Principal principal) {
        return ApiResponse.success("Current user loaded successfully", authService.currentUser(principal.getName()));
    }
}
