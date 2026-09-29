package com.rhpaie.leavebalance.mapper;

import com.rhpaie.employee.entity.Department;
import com.rhpaie.employee.entity.Employee;
import com.rhpaie.employee.entity.JobPosition;
import com.rhpaie.leavebalance.dto.LeaveBalanceResponse;
import com.rhpaie.leavebalance.dto.LeaveBalanceSummaryResponse;
import com.rhpaie.leavebalance.entity.LeaveBalance;
import org.springframework.stereotype.Component;

@Component
public class LeaveBalanceMapper {

    public LeaveBalanceResponse toResponse(LeaveBalance balance) {
        Employee employee = balance.getEmployee();
        Department department = employee.getDepartment();
        JobPosition jobPosition = employee.getJobPosition();
        return LeaveBalanceResponse.builder()
                .id(balance.getId())
                .employeeId(employee.getId())
                .employeeFullName(employee.getFirstName() + " " + employee.getLastName())
                .departmentName(department == null ? null : department.getName())
                .jobPositionName(jobPosition == null ? null : jobPosition.getTitle())
                .year(balance.getYear())
                .acquiredDays(balance.getAcquiredDays())
                .consumedDays(balance.getConsumedDays())
                .pendingDays(balance.getPendingDays())
                .remainingDays(balance.getRemainingDays())
                .createdAt(balance.getCreatedAt())
                .updatedAt(balance.getUpdatedAt())
                .build();
    }

    public LeaveBalanceSummaryResponse toSummaryResponse(LeaveBalance balance) {
        return LeaveBalanceSummaryResponse.builder()
                .year(balance.getYear())
                .acquiredDays(balance.getAcquiredDays())
                .consumedDays(balance.getConsumedDays())
                .pendingDays(balance.getPendingDays())
                .remainingDays(balance.getRemainingDays())
                .build();
    }
}
