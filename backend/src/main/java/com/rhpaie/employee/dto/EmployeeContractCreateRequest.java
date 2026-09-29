package com.rhpaie.employee.dto;

import com.rhpaie.contract.entity.ContractType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

public record EmployeeContractCreateRequest(
        @NotNull ContractType type,
        @NotNull LocalDate startDate,
        LocalDate endDate,
        LocalDate trialPeriodEndDate,
        @DecimalMin(value = "0.01", message = "Salary must be positive") BigDecimal salaryAtSignature,
        String notes,
        Boolean activateImmediately
) {
}
