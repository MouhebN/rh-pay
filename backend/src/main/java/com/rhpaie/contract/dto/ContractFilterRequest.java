package com.rhpaie.contract.dto;

import com.rhpaie.contract.entity.ContractStatus;
import com.rhpaie.contract.entity.ContractType;

import java.time.LocalDate;

public record ContractFilterRequest(
        Long employeeId,
        Long departmentId,
        Long jobPositionId,
        ContractType type,
        ContractStatus status,
        LocalDate startDateFrom,
        LocalDate startDateTo,
        LocalDate endDateFrom,
        LocalDate endDateTo,
        Integer expiringSoonDays
) {
}
