package com.rhpaie.payslip.dto;

import jakarta.validation.constraints.NotNull;

public record PayslipGenerateRequest(@NotNull Long payrollCalculationId) {
}
