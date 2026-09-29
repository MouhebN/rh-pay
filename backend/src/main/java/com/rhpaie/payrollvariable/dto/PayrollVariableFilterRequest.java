package com.rhpaie.payrollvariable.dto;

import com.rhpaie.payrollvariable.entity.PayrollVariableStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record PayrollVariableFilterRequest(
        @Min(1) @Max(12) Integer month,
        @Min(2020) @Max(2100) Integer year,
        Long employeeId,
        Long departmentId,
        Long jobPositionId,
        PayrollVariableStatus status
) {
}
