package com.rhpaie.leave.dto;

import com.rhpaie.leave.entity.LeaveStatus;
import com.rhpaie.leave.entity.LeaveType;
import lombok.Builder;

import java.time.LocalDate;

@Builder
public record LeaveRequestFilter(
        LeaveStatus status,
        LeaveType type,
        Long employeeId,
        Long departmentId,
        LocalDate startDate,
        LocalDate endDate
) {
}
