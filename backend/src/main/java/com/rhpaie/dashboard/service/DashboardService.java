package com.rhpaie.dashboard.service;

import com.rhpaie.dashboard.dto.AdminDashboardSummaryResponse;
import com.rhpaie.dashboard.dto.EmployeeDashboardSummaryResponse;

public interface DashboardService {

    AdminDashboardSummaryResponse adminSummary();

    EmployeeDashboardSummaryResponse employeeSummary(String username);
}
