package com.rhpaie.payroll.dto;

import com.rhpaie.payroll.entity.PayrollCalculationStatus;
import lombok.Builder;

import java.math.BigDecimal;

@Builder
public record PayrollCalculationResponse(
        Long id,
        Long employeeId,
        String employeeFullName,
        String departmentName,
        String jobPositionName,
        Integer month,
        Integer year,
        BigDecimal baseSalary,
        BigDecimal bonusAmount,
        BigDecimal overtimeAmount,
        BigDecimal totalDeductions,
        BigDecimal grossSalary,
        BigDecimal netSalary,
        PayrollCalculationStatus status
) {
}
