package com.rhpaie.payroll.mapper;

import com.rhpaie.employee.entity.*;
import com.rhpaie.payroll.dto.*;
import com.rhpaie.payroll.entity.PayrollCalculation;
import org.springframework.stereotype.Component;

@Component
public class PayrollCalculationMapper {

    public PayrollCalculationResponse toResponse(PayrollCalculation calculation) {
        Employee employee = calculation.getEmployee();
        Department department = employee.getDepartment();
        JobPosition jobPosition = employee.getJobPosition();
        return PayrollCalculationResponse.builder()
                .id(calculation.getId())
                .employeeId(employee.getId())
                .employeeFullName(employee.getFirstName() + " " + employee.getLastName())
                .departmentName(department == null ? null : department.getName())
                .jobPositionName(jobPosition == null ? null : jobPosition.getTitle())
                .month(calculation.getMonth())
                .year(calculation.getYear())
                .baseSalary(calculation.getBaseSalary())
                .bonusAmount(calculation.getBonusAmount())
                .overtimeAmount(calculation.getOvertimeAmount())
                .totalDeductions(calculation.getTotalDeductions())
                .grossSalary(calculation.getGrossSalary())
                .netSalary(calculation.getNetSalary())
                .status(calculation.getStatus())
                .build();
    }

    public PayrollCalculationDetailsResponse toDetails(PayrollCalculation c) {
        Employee employee = c.getEmployee();
        Department department = employee.getDepartment();
        JobPosition jobPosition = employee.getJobPosition();
        return PayrollCalculationDetailsResponse.builder()
                .id(c.getId())
                .employeeId(employee.getId())
                .employeeFullName(employee.getFirstName() + " " + employee.getLastName())
                .departmentName(department == null ? null : department.getName())
                .jobPositionName(jobPosition == null ? null : jobPosition.getTitle())
                .payrollVariableId(c.getPayrollVariable().getId())
                .month(c.getMonth())
                .year(c.getYear())
                .baseSalary(c.getBaseSalary())
                .dailyRate(c.getDailyRate())
                .hourlyRate(c.getHourlyRate())
                .unpaidAbsenceDays(c.getUnpaidAbsenceDays())
                .lateMinutes(c.getLateMinutes())
                .overtimeMinutes(c.getOvertimeMinutes())
                .bonusAmount(c.getBonusAmount())
                .advanceAmount(c.getAdvanceAmount())
                .deductionAmount(c.getDeductionAmount())
                .unpaidAbsenceDeduction(c.getUnpaidAbsenceDeduction())
                .lateDeduction(c.getLateDeduction())
                .overtimeAmount(c.getOvertimeAmount())
                .grossSalary(c.getGrossSalary())
                .totalDeductions(c.getTotalDeductions())
                .netSalary(c.getNetSalary())
                .status(c.getStatus())
                .calculatedByUsername(c.getCalculatedBy() == null ? null : c.getCalculatedBy().getUsername())
                .calculatedAt(c.getCalculatedAt())
                .finalizedByUsername(c.getFinalizedBy() == null ? null : c.getFinalizedBy().getUsername())
                .finalizedAt(c.getFinalizedAt())
                .build();
    }
}
