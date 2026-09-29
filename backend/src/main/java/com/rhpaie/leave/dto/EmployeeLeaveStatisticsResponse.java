package com.rhpaie.leave.dto;

import lombok.Builder;

import java.util.List;

@Builder
public record EmployeeLeaveStatisticsResponse(
        long myPendingRequestsCount,
        long myApprovedRequestsThisYear,
        long myRejectedRequestsThisYear,
        Double myTotalApprovedDaysThisYear,
        Double myPaidLeaveDaysThisYear,
        Double myUnpaidLeaveDaysThisYear,
        List<MonthlyLeaveStatResponse> monthlyStats,
        List<LeaveTypeStatResponse> leaveTypeStats
) {
}
