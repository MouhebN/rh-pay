package com.rhpaie.employeeexit.dto;

import com.rhpaie.employeeexit.entity.ExitStatus;
import com.rhpaie.employeeexit.entity.ExitType;
import lombok.Builder;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Builder
public record EmployeeExitResponse(
        Long id,
        Long employeeId,
        String employeeFullName,
        String departmentName,
        String jobPositionName,
        ExitType type,
        ExitStatus status,
        LocalDate exitDate,
        String reason,
        String notes,
        Boolean accountDisabled,
        Boolean activeContractTerminated,
        String createdByUsername,
        String completedByUsername,
        LocalDateTime completedAt,
        String cancelledByUsername,
        LocalDateTime cancelledAt,
        String cancellationReason,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
