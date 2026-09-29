package com.rhpaie.payroll.dto;

import jakarta.validation.constraints.*;

public record PayrollCalculationGenerateRequest(
        @NotNull @Min(1) @Max(12) Integer month,
        @NotNull @Min(2020) @Max(2100) Integer year
) {
}
