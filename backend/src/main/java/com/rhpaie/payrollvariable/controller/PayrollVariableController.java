package com.rhpaie.payrollvariable.controller;

import com.rhpaie.common.response.ApiResponse;
import com.rhpaie.payrollvariable.dto.PayrollVariableBulkValidateRequest;
import com.rhpaie.payrollvariable.dto.PayrollVariableFilterRequest;
import com.rhpaie.payrollvariable.dto.PayrollVariableGenerateRequest;
import com.rhpaie.payrollvariable.dto.PayrollVariableGenerationSummaryResponse;
import com.rhpaie.payrollvariable.dto.PayrollVariableResponse;
import com.rhpaie.payrollvariable.dto.PayrollVariableSummaryResponse;
import com.rhpaie.payrollvariable.dto.PayrollVariableUpdateRequest;
import com.rhpaie.payrollvariable.service.PayrollVariableService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.List;

@Validated
@RestController
@RequestMapping("/api/payroll-variables")
@RequiredArgsConstructor
public class PayrollVariableController {

    private final PayrollVariableService payrollVariableService;

    @PostMapping("/generate")
    @PreAuthorize("hasRole('ADMIN_RH')")
    public ApiResponse<PayrollVariableGenerationSummaryResponse> generate(
            @Valid @RequestBody PayrollVariableGenerateRequest request,
            Principal principal
    ) {
        return ApiResponse.success("Payroll variables generated successfully", payrollVariableService.generate(request, principal.getName()));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN_RH')")
    public ApiResponse<List<PayrollVariableResponse>> findAll(@Valid @ModelAttribute PayrollVariableFilterRequest filter) {
        return ApiResponse.success("Payroll variables loaded successfully", payrollVariableService.findAll(filter));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN_RH')")
    public ApiResponse<PayrollVariableResponse> findById(@PathVariable Long id) {
        return ApiResponse.success("Payroll variable loaded successfully", payrollVariableService.findById(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN_RH')")
    public ApiResponse<PayrollVariableResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody PayrollVariableUpdateRequest request
    ) {
        return ApiResponse.success("Payroll variable updated successfully", payrollVariableService.update(id, request));
    }

    @PatchMapping("/{id}/validate")
    @PreAuthorize("hasRole('ADMIN_RH')")
    public ApiResponse<PayrollVariableResponse> validate(@PathVariable Long id, Principal principal) {
        return ApiResponse.success("Payroll variable validated successfully", payrollVariableService.validate(id, principal.getName()));
    }

    @PatchMapping("/validate-bulk")
    @PreAuthorize("hasRole('ADMIN_RH')")
    public ApiResponse<List<PayrollVariableResponse>> validateBulk(
            @Valid @RequestBody PayrollVariableBulkValidateRequest request,
            Principal principal
    ) {
        return ApiResponse.success("Payroll variables validated successfully", payrollVariableService.validateBulk(request, principal.getName()));
    }

    @PatchMapping("/{id}/reopen")
    @PreAuthorize("hasRole('ADMIN_RH')")
    public ApiResponse<PayrollVariableResponse> reopen(@PathVariable Long id) {
        return ApiResponse.success("Payroll variable reopened successfully", payrollVariableService.reopen(id));
    }

    @GetMapping("/summary")
    @PreAuthorize("hasRole('ADMIN_RH')")
    public ApiResponse<PayrollVariableSummaryResponse> summary(
            @RequestParam @NotNull @Min(1) @Max(12) Integer month,
            @RequestParam @NotNull @Min(2020) @Max(2100) Integer year
    ) {
        return ApiResponse.success("Payroll variable summary loaded successfully", payrollVariableService.summary(month, year));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN_RH')")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        payrollVariableService.delete(id);
        return ApiResponse.success("Payroll variable deleted successfully");
    }
}
