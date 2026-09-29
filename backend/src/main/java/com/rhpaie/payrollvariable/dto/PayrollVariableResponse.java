package com.rhpaie.payrollvariable.dto;

import com.rhpaie.payrollvariable.entity.PayrollVariableStatus;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Builder
public record PayrollVariableResponse(
        Long id,
        Long employeeId,
        String employeeFullName,
        String departmentName,
        String jobPositionName,
        Integer month,
        Integer year,
        BigDecimal unpaidAbsenceDays,
        BigDecimal paidLeaveDays,
        BigDecimal sickLeaveDays,
        Integer lateMinutes,
        Integer overtimeMinutes,
        BigDecimal workedDays,
        BigDecimal absentDays,
        BigDecimal bonusAmount,
        BigDecimal advanceAmount,
        BigDecimal deductionAmount,
        String notes,
        PayrollVariableStatus status,
        Long generatedById,
        String generatedByUsername,
        LocalDateTime generatedAt,
        Long validatedById,
        String validatedByUsername,
        LocalDateTime validatedAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
