package com.rhpaie.employee.dto;

import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Builder
public record JobPositionResponse(
        Long id,
        String title,
        String description,
        BigDecimal baseSalary,
        DepartmentResponse department,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
