package com.rhpaie.payslip.dto;

import com.rhpaie.payslip.entity.PayslipStatus;
import jakarta.validation.constraints.*;

public record PayslipFilterRequest(
        @Min(1) @Max(12) Integer month,
        @Min(2020) @Max(2100) Integer year,
        Long employeeId,
        Long departmentId,
        Long jobPositionId,
        PayslipStatus status
) {
}
