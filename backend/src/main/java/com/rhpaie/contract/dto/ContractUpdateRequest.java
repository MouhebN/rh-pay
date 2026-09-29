package com.rhpaie.contract.dto;

import com.rhpaie.contract.entity.ContractStatus;
import com.rhpaie.contract.entity.ContractType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ContractUpdateRequest(
        @NotNull ContractType type,
        ContractStatus status,
        @NotNull LocalDate startDate,
        LocalDate endDate,
        LocalDate trialPeriodEndDate,
        @NotNull @DecimalMin(value = "0.01", message = "Salary must be positive") BigDecimal salaryAtSignature,
        @Size(max = 100) String contractReference,
        String notes
) {
}
