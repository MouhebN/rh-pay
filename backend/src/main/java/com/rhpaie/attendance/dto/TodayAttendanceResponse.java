package com.rhpaie.attendance.dto;

import com.rhpaie.attendance.entity.AttendanceStatus;
import lombok.Builder;

import java.time.LocalDate;
import java.time.LocalTime;

@Builder
public record TodayAttendanceResponse(
        LocalDate workDate,
        LocalTime checkInTime,
        LocalTime checkOutTime,
        Double workedHours,
        Integer lateMinutes,
        Integer overtimeMinutes,
        AttendanceStatus status,
        boolean canCheckIn,
        boolean canCheckOut,
        boolean onApprovedLeave,
        String message
) {
}
