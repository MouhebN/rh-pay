package com.rhpaie.attendance.dto;

import com.rhpaie.attendance.entity.AttendanceStatus;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalTime;

public record AttendanceAdjustRequest(
        @NotNull LocalDate workDate,
        LocalTime checkInTime,
        LocalTime checkOutTime,
        @NotNull AttendanceStatus status,
        String adminNote
) {
}
