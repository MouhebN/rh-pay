package com.rhpaie.leave.dto;

import com.rhpaie.leave.entity.LeaveStatus;
import com.rhpaie.leave.entity.LeaveType;
import lombok.Builder;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Builder
public record LeaveRequestResponse(
        Long id,
        Long employeeId,
        String employeeFullName,
        String departmentName,
        String jobPositionName,
        LeaveType type,
        LocalDate startDate,
        LocalDate endDate,
        Double totalDays,
        String reason,
        LeaveStatus status,
        Long reviewedById,
        String reviewedByUsername,
        LocalDateTime reviewedAt,
        String rejectionReason,
        boolean hasDocument,
        String documentFileName,
        String documentContentType,
        Long documentSize,
        LocalDateTime documentUploadedAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
