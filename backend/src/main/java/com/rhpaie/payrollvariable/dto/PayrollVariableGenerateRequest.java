package com.rhpaie.payrollvariable.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record PayrollVariableGenerateRequest(
        @NotNull @Min(1) @Max(12) Integer month,
        @NotNull @Min(2020) @Max(2100) Integer year
) {
}
