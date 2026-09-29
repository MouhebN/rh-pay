package com.rhpaie.contract.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ContractRenewRequest(
        @NotNull LocalDate newStartDate,
        LocalDate newEndDate,
        @NotNull @DecimalMin(value = "0.01", message = "Salary must be positive") BigDecimal salaryAtSignature,
        LocalDate trialPeriodEndDate,
        String notes
) {
}
