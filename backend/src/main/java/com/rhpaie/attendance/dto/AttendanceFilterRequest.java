package com.rhpaie.attendance.dto;

import com.rhpaie.attendance.entity.AttendanceSource;
import com.rhpaie.attendance.entity.AttendanceStatus;

import java.time.LocalDate;

public record AttendanceFilterRequest(
        Long employeeId,
        Long departmentId,
        Long jobPositionId,
        LocalDate startDate,
        LocalDate endDate,
        AttendanceStatus status,
        AttendanceSource source
) {
}
