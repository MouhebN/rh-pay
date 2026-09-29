package com.rhpaie.dashboard.dto;

import lombok.Builder;

import java.math.BigDecimal;
import java.util.List;

@Builder
public record AdminDashboardSummaryResponse(
        Long activeEmployeesCount,
        Long inactiveEmployeesCount,
        Long newEmployeesThisMonth,
        Long departmentsCount,
        Long jobPositionsCount,
        Long activeContractsCount,
        Long expiringSoonContractsCount,
        Long expiredContractsCount,
        Long contractsMissingDocumentCount,
        Long scheduledExitsCount,
        Long pendingLeaveRequestsCount,
        Long approvedLeaveRequestsThisMonth,
        Long employeesOnLeaveToday,
        Long leaveRequestsToReviewCount,
        Long presentTodayCount,
        Long lateTodayCount,
        Long absentTodayCount,
        Long incompleteAttendanceTodayCount,
        Integer totalOvertimeMinutesThisMonth,
        Long draftPayrollVariablesThisMonth,
        Long validatedPayrollVariablesThisMonth,
        Long draftPayrollCalculationsThisMonth,
        Long finalizedPayrollCalculationsThisMonth,
        Long generatedPayslipsThisMonth,
        BigDecimal totalNetPayrollThisMonth,
        BigDecimal totalGrossPayrollThisMonth,
        List<DashboardAlertResponse> alerts,
        List<QuickStatResponse> mainStats
) {
}
