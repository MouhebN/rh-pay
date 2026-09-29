package com.rhpaie.employee.dto;

import com.rhpaie.employee.entity.EmploymentStatus;
import com.rhpaie.employee.entity.PaymentMethod;
import com.rhpaie.user.entity.Role;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Builder
public record EmployeeResponse(
        Long id,
        String matricule,
        String firstName,
        String lastName,
        String email,
        String phone,
        LocalDate birthDate,
        LocalDate hireDate,
        DepartmentResponse department,
        JobPositionResponse jobPosition,
        BigDecimal baseSalary,
        String rib,
        String bankName,
        PaymentMethod paymentMethod,
        Long userAccountId,
        String username,
        Role userRole,
        EmploymentStatus status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
