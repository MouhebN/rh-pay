package com.rhpaie.payrollexport.dto;

import lombok.Builder;

import java.math.BigDecimal;

@Builder
public record PayrollExportSummaryResponse(
        Integer month,
        Integer year,
        Long totalEmployees,
        BigDecimal totalBaseSalary,
        BigDecimal totalGrossSalary,
        BigDecimal totalNetSalary,
        BigDecimal totalDeductions,
        BigDecimal totalBonuses,
        BigDecimal totalAdvances,
        BigDecimal totalOvertimeAmount
) {
}
