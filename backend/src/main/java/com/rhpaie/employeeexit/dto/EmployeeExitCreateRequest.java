package com.rhpaie.employeeexit.dto;

import com.rhpaie.employeeexit.entity.ExitStatus;
import com.rhpaie.employeeexit.entity.ExitType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record EmployeeExitCreateRequest(
        @NotNull Long employeeId,
        @NotNull ExitType type,
        ExitStatus status,
        @NotNull LocalDate exitDate,
        @NotBlank String reason,
        String notes
) {
}
