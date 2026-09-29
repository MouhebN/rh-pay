package com.rhpaie.payroll.dto;

import lombok.Builder;

import java.math.BigDecimal;

@Builder
public record PayrollCalculationSummaryResponse(
        Long totalEmployeesCalculated,
        Long draftCount,
        Long finalizedCount,
        Long cancelledCount,
        BigDecimal totalGrossSalary,
        BigDecimal totalNetSalary,
        BigDecimal totalDeductions,
        BigDecimal totalBonuses,
        BigDecimal totalAdvances,
        BigDecimal totalOvertimeAmount,
        BigDecimal totalUnpaidAbsenceDeduction
) {
}
