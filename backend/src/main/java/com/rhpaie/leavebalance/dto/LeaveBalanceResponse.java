package com.rhpaie.leavebalance.dto;

import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record LeaveBalanceResponse(
        Long id,
        Long employeeId,
        String employeeFullName,
        String departmentName,
        String jobPositionName,
        Integer year,
        Double acquiredDays,
        Double consumedDays,
        Double pendingDays,
        Double remainingDays,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
