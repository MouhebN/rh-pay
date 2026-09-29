package com.rhpaie.leavebalance.dto;

import lombok.Builder;

@Builder
public record LeaveBalanceSummaryResponse(
        Integer year,
        Double acquiredDays,
        Double consumedDays,
        Double pendingDays,
        Double remainingDays
) {
}
