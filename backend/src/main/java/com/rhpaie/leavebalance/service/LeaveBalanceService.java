package com.rhpaie.leavebalance.service;

import com.rhpaie.employee.entity.Employee;
import com.rhpaie.leavebalance.dto.LeaveBalanceResponse;
import com.rhpaie.leavebalance.dto.LeaveBalanceSummaryResponse;
import com.rhpaie.leavebalance.dto.LeaveBalanceUpdateRequest;

import java.util.List;

public interface LeaveBalanceService {

    List<LeaveBalanceResponse> findAll(Integer year);

    LeaveBalanceResponse findByEmployee(Long employeeId, Integer year);

    LeaveBalanceResponse updateAcquiredDays(Long employeeId, LeaveBalanceUpdateRequest request);

    LeaveBalanceSummaryResponse findMyBalance(String username, Integer year);

    void reservePendingDays(Employee employee, Integer year, Double days);

    void approvePendingDays(Employee employee, Integer year, Double days);

    void consumeApprovedDays(Employee employee, Integer year, Double days);

    void releasePendingDays(Employee employee, Integer year, Double days);

    void checkSufficientBalance(Employee employee, Integer year, Double days);

    Double getRemainingDays(Employee employee, Integer year);
}
