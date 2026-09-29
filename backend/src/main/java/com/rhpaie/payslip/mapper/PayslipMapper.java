package com.rhpaie.payslip.mapper;

import com.rhpaie.employee.entity.*;
import com.rhpaie.payslip.dto.PayslipResponse;
import com.rhpaie.payslip.entity.Payslip;
import com.rhpaie.user.entity.UserAccount;
import org.springframework.stereotype.Component;

@Component
public class PayslipMapper {

    public PayslipResponse toResponse(Payslip payslip) {
        Employee employee = payslip.getEmployee();
        Department department = employee.getDepartment();
        JobPosition jobPosition = employee.getJobPosition();
        UserAccount generatedBy = payslip.getGeneratedBy();
        return PayslipResponse.builder()
                .id(payslip.getId())
                .employeeId(employee.getId())
                .employeeFullName(employee.getFirstName() + " " + employee.getLastName())
                .departmentName(department == null ? null : department.getName())
                .jobPositionName(jobPosition == null ? null : jobPosition.getTitle())
                .payrollCalculationId(payslip.getPayrollCalculation().getId())
                .month(payslip.getMonth())
                .year(payslip.getYear())
                .fileName(payslip.getFileName())
                .contentType(payslip.getContentType())
                .fileSize(payslip.getFileSize())
                .baseSalary(payslip.getBaseSalary())
                .grossSalary(payslip.getGrossSalary())
                .totalDeductions(payslip.getTotalDeductions())
                .netSalary(payslip.getNetSalary())
                .status(payslip.getStatus())
                .generatedByUsername(generatedBy == null ? null : generatedBy.getUsername())
                .generatedAt(payslip.getGeneratedAt())
                .createdAt(payslip.getCreatedAt())
                .updatedAt(payslip.getUpdatedAt())
                .build();
    }
}
