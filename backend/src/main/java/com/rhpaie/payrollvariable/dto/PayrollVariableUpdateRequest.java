package com.rhpaie.payrollvariable.dto;

import jakarta.validation.constraints.DecimalMin;

import java.math.BigDecimal;

public record PayrollVariableUpdateRequest(
        @DecimalMin(value = "0.00") BigDecimal bonusAmount,
        @DecimalMin(value = "0.00") BigDecimal advanceAmount,
        @DecimalMin(value = "0.00") BigDecimal deductionAmount,
        String notes
) {
}
