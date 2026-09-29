package com.rhpaie.leave.dto;

import lombok.Builder;

@Builder
public record MonthlyLeaveStatResponse(
        Integer month,
        Double approvedDays,
        Long requestCount
) {
}
