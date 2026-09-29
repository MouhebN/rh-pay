package com.rhpaie.payrollexport.dto;

import lombok.Builder;

import java.math.BigDecimal;
import java.util.List;

@Builder
public record PayrollExportPreviewResponse(
        Integer month,
        Integer year,
        List<Row> rows
) {
    @Builder
    public record Row(
            String employeeFullName,
            String departmentName,
            String jobPositionName,
            BigDecimal baseSalary,
            BigDecimal grossSalary,
            BigDecimal totalDeductions,
            BigDecimal netSalary
    ) {
    }
}
