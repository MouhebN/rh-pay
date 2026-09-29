package com.rhpaie.contract.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record ContractTerminateRequest(
        @NotNull LocalDate terminationDate,
        @NotBlank String terminationReason
) {
}
