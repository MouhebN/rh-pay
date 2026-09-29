package com.rhpaie.dashboard.dto;

import lombok.Builder;

@Builder
public record DashboardAlertResponse(
        String type,
        String title,
        String message,
        Long count,
        String severity,
        String route,
        String actionLabel
) {
}
