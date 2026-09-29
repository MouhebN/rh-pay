package com.rhpaie.leave.dto;

import lombok.Builder;

import java.util.List;

@Builder
public record AdminLeaveStatisticsResponse(
        long pendingRequestsCount,
        long approvedRequestsThisMonth,
        long rejectedRequestsThisMonth,
        long cancelledRequestsThisMonth,
        Double totalApprovedLeaveDaysThisMonth,
        Double totalUnpaidLeaveDaysThisMonth,
        long employeesOnLeaveToday,
        List<MonthlyLeaveStatResponse> monthlyStats,
        List<LeaveTypeStatResponse> leaveTypeStats,
        List<DepartmentLeaveStatResponse> departmentStats
) {
}
