package com.rhpaie.contract.dto;

import com.rhpaie.contract.entity.ContractStatus;
import com.rhpaie.contract.entity.ContractType;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Builder
public record ContractResponse(
        Long id,
        Long employeeId,
        String employeeFullName,
        String departmentName,
        String jobPositionName,
        ContractType type,
        ContractStatus status,
        LocalDate startDate,
        LocalDate endDate,
        LocalDate trialPeriodEndDate,
        BigDecimal salaryAtSignature,
        String contractReference,
        String notes,
        String documentFileName,
        String documentContentType,
        Long documentSize,
        LocalDateTime documentUploadedAt,
        String createdByUsername,
        String updatedByUsername,
        String terminatedByUsername,
        LocalDate terminationDate,
        String terminationReason,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
