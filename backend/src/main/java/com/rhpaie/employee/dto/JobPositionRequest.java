package com.rhpaie.employee.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;

import java.math.BigDecimal;

@Builder
public record JobPositionRequest(
        @NotBlank(message = "Job title is required")
        @Size(max = 120, message = "Job title must not exceed 120 characters")
        String title,

        @Size(max = 500, message = "Description must not exceed 500 characters")
        String description,

        @DecimalMin(value = "0.0", message = "Base salary must be positive")
        BigDecimal baseSalary,

        Long departmentId
) {
}
