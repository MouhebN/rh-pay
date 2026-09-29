package com.rhpaie.employee.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;

@Builder
public record DepartmentRequest(
        @NotBlank(message = "Department name is required")
        @Size(max = 120, message = "Department name must not exceed 120 characters")
        String name,

        @Size(max = 500, message = "Description must not exceed 500 characters")
        String description
) {
}
