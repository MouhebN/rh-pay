package com.rhpaie.payrollvariable.mapper;

import com.rhpaie.employee.entity.Department;
import com.rhpaie.employee.entity.Employee;
import com.rhpaie.employee.entity.JobPosition;
import com.rhpaie.payrollvariable.dto.PayrollVariableResponse;
import com.rhpaie.payrollvariable.entity.PayrollVariable;
import com.rhpaie.user.entity.UserAccount;
import org.springframework.stereotype.Component;

@Component
public class PayrollVariableMapper {

    public PayrollVariableResponse toResponse(PayrollVariable variable) {
        Employee employee = variable.getEmployee();
        Department department = employee.getDepartment();
        JobPosition jobPosition = employee.getJobPosition();
        UserAccount generatedBy = variable.getGeneratedBy();
        UserAccount validatedBy = variable.getValidatedBy();

        return PayrollVariableResponse.builder()
                .id(variable.getId())
                .employeeId(employee.getId())
                .employeeFullName(employee.getFirstName() + " " + employee.getLastName())
                .departmentName(department == null ? null : department.getName())
                .jobPositionName(jobPosition == null ? null : jobPosition.getTitle())
                .month(variable.getMonth())
                .year(variable.getYear())
                .unpaidAbsenceDays(variable.getUnpaidAbsenceDays())
                .paidLeaveDays(variable.getPaidLeaveDays())
                .sickLeaveDays(variable.getSickLeaveDays())
                .lateMinutes(variable.getLateMinutes())
                .overtimeMinutes(variable.getOvertimeMinutes())
                .workedDays(variable.getWorkedDays())
                .absentDays(variable.getAbsentDays())
                .bonusAmount(variable.getBonusAmount())
                .advanceAmount(variable.getAdvanceAmount())
                .deductionAmount(variable.getDeductionAmount())
                .notes(variable.getNotes())
                .status(variable.getStatus())
                .generatedById(generatedBy == null ? null : generatedBy.getId())
                .generatedByUsername(generatedBy == null ? null : generatedBy.getUsername())
                .generatedAt(variable.getGeneratedAt())
                .validatedById(validatedBy == null ? null : validatedBy.getId())
                .validatedByUsername(validatedBy == null ? null : validatedBy.getUsername())
                .validatedAt(variable.getValidatedAt())
                .createdAt(variable.getCreatedAt())
                .updatedAt(variable.getUpdatedAt())
                .build();
    }
}
