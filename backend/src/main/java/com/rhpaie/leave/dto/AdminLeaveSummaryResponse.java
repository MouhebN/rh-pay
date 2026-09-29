package com.rhpaie.leave.dto;

import lombok.Builder;

@Builder
public record AdminLeaveSummaryResponse(
        long pendingCount,
        long approvedThisMonth,
        long rejectedThisMonth,
        long cancelledThisMonth
) {
}
