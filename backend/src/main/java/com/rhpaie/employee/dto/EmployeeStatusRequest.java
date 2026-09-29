package com.rhpaie.employee.dto;

import com.rhpaie.employee.entity.EmploymentStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

@Builder
public record EmployeeStatusRequest(
        @NotNull(message = "Employment status is required")
        EmploymentStatus status
) {
}
