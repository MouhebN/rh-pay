package com.rhpaie.contract.mapper;

import com.rhpaie.contract.dto.ContractResponse;
import com.rhpaie.contract.entity.Contract;
import com.rhpaie.employee.entity.Employee;
import org.springframework.stereotype.Component;

@Component
public class ContractMapper {

    public ContractResponse toResponse(Contract contract) {
        Employee employee = contract.getEmployee();
        return ContractResponse.builder()
                .id(contract.getId())
                .employeeId(employee.getId())
                .employeeFullName(employee.getFirstName() + " " + employee.getLastName())
                .departmentName(employee.getDepartment() == null ? null : employee.getDepartment().getName())
                .jobPositionName(employee.getJobPosition() == null ? null : employee.getJobPosition().getTitle())
                .type(contract.getType())
                .status(contract.getStatus())
                .startDate(contract.getStartDate())
                .endDate(contract.getEndDate())
                .trialPeriodEndDate(contract.getTrialPeriodEndDate())
                .salaryAtSignature(contract.getSalaryAtSignature())
                .contractReference(contract.getContractReference())
                .notes(contract.getNotes())
                .documentFileName(contract.getDocumentFileName())
                .documentContentType(contract.getDocumentContentType())
                .documentSize(contract.getDocumentSize())
                .documentUploadedAt(contract.getDocumentUploadedAt())
                .createdByUsername(contract.getCreatedBy() == null ? null : contract.getCreatedBy().getUsername())
                .updatedByUsername(contract.getUpdatedBy() == null ? null : contract.getUpdatedBy().getUsername())
                .terminatedByUsername(contract.getTerminatedBy() == null ? null : contract.getTerminatedBy().getUsername())
                .terminationDate(contract.getTerminationDate())
                .terminationReason(contract.getTerminationReason())
                .createdAt(contract.getCreatedAt())
                .updatedAt(contract.getUpdatedAt())
                .build();
    }
}
