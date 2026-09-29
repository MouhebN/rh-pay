package com.rhpaie.payslip.dto;

import com.rhpaie.payslip.entity.PayslipStatus;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Builder
public record PayslipResponse(
        Long id,
        Long employeeId,
        String employeeFullName,
        String departmentName,
        String jobPositionName,
        Long payrollCalculationId,
        Integer month,
        Integer year,
        String fileName,
        String contentType,
        Long fileSize,
        BigDecimal baseSalary,
        BigDecimal grossSalary,
        BigDecimal totalDeductions,
        BigDecimal netSalary,
        PayslipStatus status,
        String generatedByUsername,
        LocalDateTime generatedAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
