package com.rhpaie.user.controller;

import com.rhpaie.common.response.ApiResponse;
import com.rhpaie.common.security.SecurityConstants;
import com.rhpaie.user.dto.UserRequest;
import com.rhpaie.user.dto.UserResponse;
import com.rhpaie.user.dto.UserStatusRequest;
import com.rhpaie.user.dto.UserUpdateRequest;
import com.rhpaie.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping(SecurityConstants.USERS_BASE_PATH)
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/health")
    public ApiResponse<String> health() {
        return ApiResponse.success("User module is available", userService.health());
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN_RH')")
    public ApiResponse<List<UserResponse>> findAll() {
        return ApiResponse.success("Users loaded successfully", userService.findAll());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN_RH')")
    public ApiResponse<UserResponse> findById(@PathVariable Long id) {
        return ApiResponse.success("User loaded successfully", userService.findById(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN_RH')")
    public ApiResponse<UserResponse> create(@Valid @RequestBody UserRequest request) {
        return ApiResponse.success("User created successfully", userService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN_RH')")
    public ApiResponse<UserResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody UserUpdateRequest request
    ) {
        return ApiResponse.success("User updated successfully", userService.update(id, request));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN_RH')")
    public ApiResponse<UserResponse> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody UserStatusRequest request
    ) {
        return ApiResponse.success("User status updated successfully", userService.updateStatus(id, request));
    }

    @PatchMapping("/{id}/unlock")
    @PreAuthorize("hasRole('ADMIN_RH')")
    public ApiResponse<UserResponse> unlock(@PathVariable Long id) {
        return ApiResponse.success("User unlocked successfully", userService.unlock(id));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN_RH')")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        userService.delete(id);
        return ApiResponse.success("User deleted successfully");
    }
}
