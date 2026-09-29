package com.rhpaie.leave.dto;

import lombok.Builder;

@Builder
public record EmployeeLeaveSummaryResponse(
        long pendingRequests,
        long approvedThisYear,
        long rejectedThisYear
) {
}
