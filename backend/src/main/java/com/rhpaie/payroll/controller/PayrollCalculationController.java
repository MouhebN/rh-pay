package com.rhpaie.payroll.controller;

import com.rhpaie.common.response.ApiResponse;
import com.rhpaie.payroll.dto.*;
import com.rhpaie.payroll.service.PayrollCalculationService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@Validated
@RestController
@RequestMapping("/api/payroll-calculations")
@RequiredArgsConstructor
public class PayrollCalculationController {

    private final PayrollCalculationService payrollCalculationService;

    @PostMapping("/generate")
    @PreAuthorize("hasRole('ADMIN_RH')")
    public ApiResponse<PayrollCalculationGenerationSummaryResponse> generate(@Valid @RequestBody PayrollCalculationGenerateRequest request, Principal principal) {
        return ApiResponse.success("Payroll calculations generated successfully", payrollCalculationService.generate(request, principal.getName()));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN_RH')")
    public ApiResponse<List<PayrollCalculationResponse>> findAll(@Valid @ModelAttribute PayrollCalculationFilterRequest filter) {
        return ApiResponse.success("Payroll calculations loaded successfully", payrollCalculationService.findAll(filter));
    }

    @GetMapping("/summary")
    @PreAuthorize("hasRole('ADMIN_RH')")
    public ApiResponse<PayrollCalculationSummaryResponse> summary(@RequestParam @NotNull @Min(1) @Max(12) Integer month, @RequestParam @NotNull @Min(2020) @Max(2100) Integer year) {
        return ApiResponse.success("Payroll calculation summary loaded successfully", payrollCalculationService.summary(month, year));
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('EMPLOYE')")
    public ApiResponse<List<PayrollCalculationResponse>> mine(@RequestParam(required = false) Integer year, Principal principal) {
        return ApiResponse.success("Salaries loaded successfully", payrollCalculationService.findMine(principal.getName(), year));
    }

    @GetMapping("/me/{id}")
    @PreAuthorize("hasRole('EMPLOYE')")
    public ApiResponse<PayrollCalculationDetailsResponse> mineById(@PathVariable Long id, Principal principal) {
        return ApiResponse.success("Salary loaded successfully", payrollCalculationService.findMineById(principal.getName(), id));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN_RH')")
    public ApiResponse<PayrollCalculationDetailsResponse> findById(@PathVariable Long id) {
        return ApiResponse.success("Payroll calculation loaded successfully", payrollCalculationService.findById(id));
    }

    @PatchMapping("/{id}/finalize")
    @PreAuthorize("hasRole('ADMIN_RH')")
    public ApiResponse<PayrollCalculationDetailsResponse> finalizeOne(@PathVariable Long id, Principal principal) {
        return ApiResponse.success("Payroll calculation finalized successfully", payrollCalculationService.finalizeCalculation(id, principal.getName()));
    }

    @PatchMapping("/finalize-bulk")
    @PreAuthorize("hasRole('ADMIN_RH')")
    public ApiResponse<List<PayrollCalculationResponse>> finalizeBulk(@Valid @RequestBody PayrollCalculationBulkFinalizeRequest request, Principal principal) {
        return ApiResponse.success("Payroll calculations finalized successfully", payrollCalculationService.finalizeBulk(request, principal.getName()));
    }

    @PatchMapping("/{id}/cancel")
    @PreAuthorize("hasRole('ADMIN_RH')")
    public ApiResponse<PayrollCalculationDetailsResponse> cancel(@PathVariable Long id) {
        return ApiResponse.success("Payroll calculation cancelled successfully", payrollCalculationService.cancel(id));
    }
}
