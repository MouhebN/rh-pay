package com.rhpaie.settings.controller;

import com.rhpaie.common.response.ApiResponse;
import com.rhpaie.settings.dto.AppSettingsResponse;
import com.rhpaie.settings.dto.AppSettingsUpdateRequest;
import com.rhpaie.settings.service.SettingsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;

@RestController
@RequestMapping("/api/settings")
@RequiredArgsConstructor
public class SettingsController {

    private final SettingsService settingsService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN_RH')")
    public ApiResponse<AppSettingsResponse> get() {
        return ApiResponse.success("Settings loaded successfully", settingsService.get());
    }

    @PutMapping
    @PreAuthorize("hasRole('ADMIN_RH')")
    public ApiResponse<AppSettingsResponse> update(@Valid @RequestBody AppSettingsUpdateRequest request, Principal principal) {
        return ApiResponse.success("Settings updated successfully", settingsService.update(request, principal.getName()));
    }

    @PostMapping("/reset-defaults")
    @PreAuthorize("hasRole('ADMIN_RH')")
    public ApiResponse<AppSettingsResponse> resetDefaults(Principal principal) {
        return ApiResponse.success("Settings reset successfully", settingsService.resetDefaults(principal.getName()));
    }
}
