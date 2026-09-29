package com.rhpaie.employeeexit.dto;

import com.rhpaie.employeeexit.entity.ExitStatus;
import com.rhpaie.employeeexit.entity.ExitType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record EmployeeExitUpdateRequest(
        @NotNull ExitType type,
        @NotNull ExitStatus status,
        @NotNull LocalDate exitDate,
        @NotBlank String reason,
        String notes
) {
}
