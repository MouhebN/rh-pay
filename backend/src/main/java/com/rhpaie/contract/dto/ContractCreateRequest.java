package com.rhpaie.contract.dto;

import com.rhpaie.contract.entity.ContractStatus;
import com.rhpaie.contract.entity.ContractType;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ContractCreateRequest(
        @NotNull Long employeeId,
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
