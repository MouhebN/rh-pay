package com.rhpaie.settings.dto;

import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Builder
public record AppSettingsResponse(
        Long id,
        String companyName,
        String companyAddress,
        String companyEmail,
        String companyPhone,
        LocalTime workStartTime,
        LocalTime workEndTime,
        BigDecimal expectedDailyHours,
        BigDecimal monthlyReferenceHours,
        BigDecimal defaultAnnualLeaveDays,
        Boolean allowNegativeLeaveBalance,
        BigDecimal overtimeMultiplier,
        Boolean deductLateMinutesFromSalary,
        String currency,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        Long updatedById,
        String updatedByUsername
) {
}
