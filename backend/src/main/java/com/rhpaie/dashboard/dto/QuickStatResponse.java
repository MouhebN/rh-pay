package com.rhpaie.dashboard.dto;

import lombok.Builder;

@Builder
public record QuickStatResponse(
        String label,
        String value,
        String helper,
        String icon,
        String severity,
        String route
) {
}
