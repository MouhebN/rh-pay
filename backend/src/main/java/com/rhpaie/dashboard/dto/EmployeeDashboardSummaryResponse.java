package com.rhpaie.dashboard.dto;

import com.rhpaie.attendance.entity.AttendanceStatus;
import com.rhpaie.contract.entity.ContractType;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

@Builder
public record EmployeeDashboardSummaryResponse(
        String employeeFullName,
        String departmentName,
        String jobPositionName,
        ContractType currentContractType,
        LocalDate currentContractEndDate,
        Double leaveBalanceRemaining,
        Long pendingLeaveRequestsCount,
        Long approvedLeaveRequestsThisYear,
        AttendanceStatus todayAttendanceStatus,
        LocalTime todayCheckInTime,
        LocalTime todayCheckOutTime,
        Double workedHoursToday,
        BigDecimal latestNetSalary,
        Integer latestSalaryMonth,
        Integer latestSalaryYear,
        Boolean latestPayslipAvailable
) {
}
