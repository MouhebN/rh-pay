package com.rhpaie.leave.dto;

import lombok.Builder;

import java.util.List;

@Builder
public record LeaveValidationResponse(
        boolean valid,
        List<String> blockingErrors,
        List<String> warnings,
        Double requestedDays,
        Double remainingLeaveBalance,
        Boolean affectsPayroll
) {
}
