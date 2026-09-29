package com.rhpaie.employee.controller;

import com.rhpaie.common.response.ApiResponse;
import com.rhpaie.employee.dto.JobPositionRequest;
import com.rhpaie.employee.dto.JobPositionResponse;
import com.rhpaie.employee.service.JobPositionService;
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
@RequestMapping("/api/job-positions")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN_RH')")
public class JobPositionController {

    private final JobPositionService jobPositionService;

    @GetMapping
    public ApiResponse<List<JobPositionResponse>> findAll() {
        return ApiResponse.success("Job positions loaded successfully", jobPositionService.findAll());
    }

    @GetMapping("/{id}")
    public ApiResponse<JobPositionResponse> findById(@PathVariable Long id) {
        return ApiResponse.success("Job position loaded successfully", jobPositionService.findById(id));
    }

    @PostMapping
    public ApiResponse<JobPositionResponse> create(@Valid @RequestBody JobPositionRequest request) {
        return ApiResponse.success("Job position created successfully", jobPositionService.create(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<JobPositionResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody JobPositionRequest request
    ) {
        return ApiResponse.success("Job position updated successfully", jobPositionService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        jobPositionService.delete(id);
        return ApiResponse.success("Job position deleted successfully");
    }
}
