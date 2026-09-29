package com.rhpaie.settings.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalTime;

public record AppSettingsUpdateRequest(
        @NotBlank String companyName,
        String companyAddress,
        String companyEmail,
        String companyPhone,
        @NotNull LocalTime workStartTime,
        @NotNull LocalTime workEndTime,
        @NotNull @DecimalMin(value = "0.01") BigDecimal expectedDailyHours,
        @NotNull @DecimalMin(value = "0.01") BigDecimal monthlyReferenceHours,
        @NotNull @DecimalMin(value = "0.00") BigDecimal defaultAnnualLeaveDays,
        @NotNull Boolean allowNegativeLeaveBalance,
        @NotNull @DecimalMin(value = "1.00") BigDecimal overtimeMultiplier,
        @NotNull Boolean deductLateMinutesFromSalary,
        @NotBlank String currency
) {
}
