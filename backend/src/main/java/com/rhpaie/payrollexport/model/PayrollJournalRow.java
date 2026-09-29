package com.rhpaie.payrollexport.model;

import com.rhpaie.payroll.entity.PayrollCalculationStatus;

import java.math.BigDecimal;

public record PayrollJournalRow(
        String matricule,
        String employeeFullName,
        String email,
        String departmentName,
        String jobPositionName,
        Integer month,
        Integer year,
        BigDecimal baseSalary,
        BigDecimal dailyRate,
        BigDecimal hourlyRate,
        BigDecimal unpaidAbsenceDays,
        BigDecimal unpaidAbsenceDeduction,
        Integer lateMinutes,
        BigDecimal lateDeduction,
        Integer overtimeMinutes,
        BigDecimal overtimeAmount,
        BigDecimal bonusAmount,
        BigDecimal advanceAmount,
        BigDecimal deductionAmount,
        BigDecimal grossSalary,
        BigDecimal totalDeductions,
        BigDecimal netSalary,
        PayrollCalculationStatus status
) {
}
