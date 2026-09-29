package com.rhpaie.employeeexit.dto;

import jakarta.validation.constraints.NotBlank;

public record EmployeeExitCancelRequest(
        @NotBlank String cancellationReason
) {
}
