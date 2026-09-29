package com.rhpaie.payslip.dto;

import jakarta.validation.constraints.*;

public record PayslipBulkGenerateRequest(
        @NotNull @Min(1) @Max(12) Integer month,
        @NotNull @Min(2020) @Max(2100) Integer year
) {
}
