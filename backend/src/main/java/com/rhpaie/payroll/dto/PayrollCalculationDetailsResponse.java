package com.rhpaie.payroll.dto;

import com.rhpaie.payroll.entity.PayrollCalculationStatus;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Builder
public record PayrollCalculationDetailsResponse(
        Long id,
        Long employeeId,
        String employeeFullName,
        String departmentName,
        String jobPositionName,
        Long payrollVariableId,
        Integer month,
        Integer year,
        BigDecimal baseSalary,
        BigDecimal dailyRate,
        BigDecimal hourlyRate,
        BigDecimal unpaidAbsenceDays,
        Integer lateMinutes,
        Integer overtimeMinutes,
        BigDecimal bonusAmount,
        BigDecimal advanceAmount,
        BigDecimal deductionAmount,
        BigDecimal unpaidAbsenceDeduction,
        BigDecimal lateDeduction,
        BigDecimal overtimeAmount,
        BigDecimal grossSalary,
        BigDecimal totalDeductions,
        BigDecimal netSalary,
        PayrollCalculationStatus status,
        String calculatedByUsername,
        LocalDateTime calculatedAt,
        String finalizedByUsername,
        LocalDateTime finalizedAt
) {
}
