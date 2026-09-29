package com.rhpaie.employeeexit.service;

import com.rhpaie.employeeexit.dto.*;

import java.util.List;

public interface EmployeeExitService {
    EmployeeExitResponse create(EmployeeExitCreateRequest request, String username);
    List<EmployeeExitResponse> findAll(EmployeeExitFilterRequest filter);
    EmployeeExitResponse findById(Long id);
    EmployeeExitResponse update(Long id, EmployeeExitUpdateRequest request);
    EmployeeExitResponse complete(Long id, EmployeeExitCompleteRequest request, String username);
    EmployeeExitResponse cancel(Long id, EmployeeExitCancelRequest request, String username);
    List<EmployeeExitResponse> findByEmployee(Long employeeId);
    List<EmployeeExitResponse> scheduled();
    List<EmployeeExitResponse> upcoming(Integer days);
    EmployeeExitSummaryResponse summary();
    int processDue(String username);
}
