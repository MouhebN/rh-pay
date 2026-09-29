package com.rhpaie.leavebalance.controller;

import com.rhpaie.common.response.ApiResponse;
import com.rhpaie.leavebalance.dto.LeaveBalanceResponse;
import com.rhpaie.leavebalance.dto.LeaveBalanceSummaryResponse;
import com.rhpaie.leavebalance.dto.LeaveBalanceUpdateRequest;
import com.rhpaie.leavebalance.service.LeaveBalanceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/leave-balances")
@RequiredArgsConstructor
public class LeaveBalanceController {

    private final LeaveBalanceService leaveBalanceService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN_RH')")
    public ApiResponse<List<LeaveBalanceResponse>> findAll(@RequestParam(required = false) Integer year) {
        return ApiResponse.success("Leave balances loaded successfully", leaveBalanceService.findAll(year));
    }

    @GetMapping("/employee/{employeeId}")
    @PreAuthorize("hasRole('ADMIN_RH')")
    public ApiResponse<LeaveBalanceResponse> findByEmployee(
            @PathVariable Long employeeId,
            @RequestParam(required = false) Integer year
    ) {
        return ApiResponse.success("Leave balance loaded successfully", leaveBalanceService.findByEmployee(employeeId, year));
    }

    @PutMapping("/employee/{employeeId}")
    @PreAuthorize("hasRole('ADMIN_RH')")
    public ApiResponse<LeaveBalanceResponse> updateAcquiredDays(
            @PathVariable Long employeeId,
            @Valid @RequestBody LeaveBalanceUpdateRequest request
    ) {
        return ApiResponse.success("Leave balance updated successfully", leaveBalanceService.updateAcquiredDays(employeeId, request));
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('EMPLOYE')")
    public ApiResponse<LeaveBalanceSummaryResponse> findMyBalance(
            @RequestParam(required = false) Integer year,
            Principal principal
    ) {
        return ApiResponse.success("Leave balance loaded successfully", leaveBalanceService.findMyBalance(principal.getName(), year));
    }
}
