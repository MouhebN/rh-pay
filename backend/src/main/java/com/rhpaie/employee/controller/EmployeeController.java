package com.rhpaie.employee.controller;

import com.rhpaie.common.response.ApiResponse;
import com.rhpaie.employee.dto.EmployeeRequest;
import com.rhpaie.employee.dto.EmployeeResponse;
import com.rhpaie.employee.dto.EmployeeStatusRequest;
import com.rhpaie.employee.service.EmployeeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/employees")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN_RH')")
public class EmployeeController {

    private final EmployeeService employeeService;

    @GetMapping
    public ApiResponse<List<EmployeeResponse>> findAll() {
        return ApiResponse.success("Employees loaded successfully", employeeService.findAll());
    }

    @GetMapping("/{id}")
    public ApiResponse<EmployeeResponse> findById(@PathVariable Long id) {
        return ApiResponse.success("Employee loaded successfully", employeeService.findById(id));
    }

    @PostMapping
    public ApiResponse<EmployeeResponse> create(@Valid @RequestBody EmployeeRequest request, Principal principal) {
        return ApiResponse.success("Employee created successfully", employeeService.create(request, principal.getName()));
    }

    @PutMapping("/{id}")
    public ApiResponse<EmployeeResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody EmployeeRequest request
    ) {
        return ApiResponse.success("Employee updated successfully", employeeService.update(id, request));
    }

    @PatchMapping("/{id}/status")
    public ApiResponse<EmployeeResponse> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody EmployeeStatusRequest request
    ) {
        return ApiResponse.success("Employee status updated successfully", employeeService.updateStatus(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        employeeService.delete(id);
        return ApiResponse.success("Employee deleted successfully");
    }
}
