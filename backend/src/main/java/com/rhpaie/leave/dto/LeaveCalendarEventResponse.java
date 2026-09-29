package com.rhpaie.leave.dto;

import com.rhpaie.leave.entity.LeaveStatus;
import com.rhpaie.leave.entity.LeaveType;
import lombok.Builder;

import java.time.LocalDate;

@Builder
public record LeaveCalendarEventResponse(
        Long leaveRequestId,
        Long employeeId,
        String employeeFullName,
        String departmentName,
        String jobPositionName,
        LeaveType type,
        LeaveStatus status,
        LocalDate startDate,
        LocalDate endDate,
        Double totalDays,
        String title,
        String color
) {
}
