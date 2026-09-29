package com.rhpaie.employeeexit.mapper;

import com.rhpaie.employee.entity.Employee;
import com.rhpaie.employeeexit.dto.EmployeeExitResponse;
import com.rhpaie.employeeexit.entity.EmployeeExit;
import org.springframework.stereotype.Component;

@Component
public class EmployeeExitMapper {

    public EmployeeExitResponse toResponse(EmployeeExit exit) {
        Employee employee = exit.getEmployee();
        return EmployeeExitResponse.builder()
                .id(exit.getId())
                .employeeId(employee.getId())
                .employeeFullName(employee.getFirstName() + " " + employee.getLastName())
                .departmentName(employee.getDepartment() == null ? null : employee.getDepartment().getName())
                .jobPositionName(employee.getJobPosition() == null ? null : employee.getJobPosition().getTitle())
                .type(exit.getType())
                .status(exit.getStatus())
                .exitDate(exit.getExitDate())
                .reason(exit.getReason())
                .notes(exit.getNotes())
                .accountDisabled(exit.getAccountDisabled())
                .activeContractTerminated(exit.getActiveContractTerminated())
                .createdByUsername(exit.getCreatedBy() == null ? null : exit.getCreatedBy().getUsername())
                .completedByUsername(exit.getCompletedBy() == null ? null : exit.getCompletedBy().getUsername())
                .completedAt(exit.getCompletedAt())
                .cancelledByUsername(exit.getCancelledBy() == null ? null : exit.getCancelledBy().getUsername())
                .cancelledAt(exit.getCancelledAt())
                .cancellationReason(exit.getCancellationReason())
                .createdAt(exit.getCreatedAt())
                .updatedAt(exit.getUpdatedAt())
                .build();
    }
}
