package com.rhpaie.employeeexit.dto;

import lombok.Builder;

import java.util.Map;

@Builder
public record EmployeeExitSummaryResponse(
        Long totalExits,
        Long draftCount,
        Long scheduledCount,
        Long completedCount,
        Long cancelledCount,
        Long upcomingCount,
        Map<String, Long> exitsByType
) {
}
