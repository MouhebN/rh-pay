package com.rhpaie.attendance.dto;

import lombok.Builder;

@Builder
public record AttendanceSummaryResponse(
        Long presentTodayCount,
        Long lateTodayCount,
        Long absentTodayCount,
        Long onLeaveTodayCount,
        Long incompleteTodayCount,
        Integer totalOvertimeMinutesThisMonth,
        Double totalWorkedHoursThisMonth
) {
}
