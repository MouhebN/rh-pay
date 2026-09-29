package com.rhpaie.employee.dto;

import com.rhpaie.employee.entity.EmploymentStatus;
import com.rhpaie.employee.entity.PaymentMethod;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDate;

@Builder
public record EmployeeRequest(
        @NotBlank(message = "Matricule is required")
        @Size(max = 50, message = "Matricule must not exceed 50 characters")
        String matricule,

        @NotBlank(message = "First name is required")
        @Size(max = 100, message = "First name must not exceed 100 characters")
        String firstName,

        @NotBlank(message = "Last name is required")
        @Size(max = 100, message = "Last name must not exceed 100 characters")
        String lastName,

        @NotBlank(message = "Email is required")
        @Email(message = "Email must be valid")
        @Size(max = 150, message = "Email must not exceed 150 characters")
        String email,

        @Size(max = 30, message = "Phone must not exceed 30 characters")
        String phone,

        @Past(message = "Birth date must be in the past")
        LocalDate birthDate,

        @NotNull(message = "Hire date is required")
        @PastOrPresent(message = "Hire date cannot be in the future")
        LocalDate hireDate,

        Long departmentId,
        Long jobPositionId,
        BigDecimal baseSalary,

        @Size(max = 50, message = "RIB must not exceed 50 characters")
        String rib,

        @Size(max = 120, message = "Bank name must not exceed 120 characters")
        String bankName,

        PaymentMethod paymentMethod,

        Long userAccountId,
        EmploymentStatus status,
        Boolean createContract,
        @Valid
        EmployeeContractCreateRequest contract
) {
}
