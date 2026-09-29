package com.rhpaie.dashboard.controller;

import com.rhpaie.common.response.ApiResponse;
import com.rhpaie.dashboard.dto.AdminDashboardSummaryResponse;
import com.rhpaie.dashboard.dto.EmployeeDashboardSummaryResponse;
import com.rhpaie.dashboard.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/admin-summary")
    @PreAuthorize("hasRole('ADMIN_RH')")
    public ApiResponse<AdminDashboardSummaryResponse> adminSummary() {
        return ApiResponse.success("Admin dashboard summary loaded successfully", dashboardService.adminSummary());
    }

    @GetMapping("/employee-summary")
    @PreAuthorize("hasRole('EMPLOYE')")
    public ApiResponse<EmployeeDashboardSummaryResponse> employeeSummary(Principal principal) {
        return ApiResponse.success("Employee dashboard summary loaded successfully", dashboardService.employeeSummary(principal.getName()));
    }
}
