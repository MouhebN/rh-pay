package com.rhpaie.employee.controller;

import com.rhpaie.common.response.ApiResponse;
import com.rhpaie.employee.dto.DepartmentRequest;
import com.rhpaie.employee.dto.DepartmentResponse;
import com.rhpaie.employee.service.DepartmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/departments")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN_RH')")
public class DepartmentController {

    private final DepartmentService departmentService;

    @GetMapping
    public ApiResponse<List<DepartmentResponse>> findAll() {
        return ApiResponse.success("Departments loaded successfully", departmentService.findAll());
    }

    @GetMapping("/{id}")
    public ApiResponse<DepartmentResponse> findById(@PathVariable Long id) {
        return ApiResponse.success("Department loaded successfully", departmentService.findById(id));
    }

    @PostMapping
    public ApiResponse<DepartmentResponse> create(@Valid @RequestBody DepartmentRequest request) {
        return ApiResponse.success("Department created successfully", departmentService.create(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<DepartmentResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody DepartmentRequest request
    ) {
        return ApiResponse.success("Department updated successfully", departmentService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        departmentService.delete(id);
        return ApiResponse.success("Department deleted successfully");
    }
}
