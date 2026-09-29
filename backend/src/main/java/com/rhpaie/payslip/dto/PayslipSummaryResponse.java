package com.rhpaie.payslip.dto;

import lombok.Builder;

import java.math.BigDecimal;

@Builder
public record PayslipSummaryResponse(
        Long totalPayslips,
        Long generatedCount,
        Long cancelledCount,
        BigDecimal totalNetSalary,
        BigDecimal totalGrossSalary,
        BigDecimal totalDeductions
) {
}
