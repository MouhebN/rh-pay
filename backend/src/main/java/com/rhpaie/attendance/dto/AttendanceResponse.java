package com.rhpaie.attendance.dto;

import com.rhpaie.attendance.entity.AttendanceSource;
import com.rhpaie.attendance.entity.AttendanceStatus;
import lombok.Builder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Builder
public record AttendanceResponse(
        Long id,
        Long employeeId,
        String employeeFullName,
        String departmentName,
        String jobPositionName,
        LocalDate workDate,
        LocalTime checkInTime,
        LocalTime checkOutTime,
        Double workedHours,
        Integer lateMinutes,
        Integer overtimeMinutes,
        AttendanceStatus status,
        AttendanceSource source,
        String adminNote,
        Long adjustedById,
        String adjustedByUsername,
        LocalDateTime adjustedAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
