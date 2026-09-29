package com.rhpaie.notification.controller;

import com.rhpaie.common.response.ApiResponse;
import com.rhpaie.notification.dto.NotificationResponse;
import com.rhpaie.notification.service.AppNotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final AppNotificationService notificationService;

    @GetMapping("/recent")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<List<NotificationResponse>> recent(Principal principal) {
        return ApiResponse.success("Notifications loaded successfully", notificationService.recent(principal.getName()));
    }

    @PostMapping("/read")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<Void> markAllRead(Principal principal) {
        notificationService.markAllRead(principal.getName());
        return ApiResponse.success("Notifications marked as read");
    }
}
