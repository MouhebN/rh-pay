package com.rhpaie.payrollvariable.dto;

import lombok.Builder;

import java.math.BigDecimal;

@Builder
public record PayrollVariableSummaryResponse(
        Long totalEmployees,
        Long draftCount,
        Long validatedCount,
        Long lockedCount,
        BigDecimal totalBonusAmount,
        BigDecimal totalAdvanceAmount,
        BigDecimal totalDeductionAmount,
        BigDecimal totalUnpaidAbsenceDays,
        Integer totalLateMinutes,
        Integer totalOvertimeMinutes
) {
}
