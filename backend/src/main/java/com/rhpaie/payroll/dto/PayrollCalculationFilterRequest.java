package com.rhpaie.payroll.dto;

import com.rhpaie.payroll.entity.PayrollCalculationStatus;
import jakarta.validation.constraints.*;

public record PayrollCalculationFilterRequest(
        @Min(1) @Max(12) Integer month,
        @Min(2020) @Max(2100) Integer year,
        Long employeeId,
        Long departmentId,
        Long jobPositionId,
        PayrollCalculationStatus status
) {
}
