package com.rhpaie.payslip.controller;

import com.rhpaie.common.response.ApiResponse;
import com.rhpaie.payslip.dto.*;
import com.rhpaie.payslip.service.PayslipService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@Validated
@RestController
@RequestMapping("/api/payslips")
@RequiredArgsConstructor
public class PayslipController {

    private final PayslipService payslipService;

    @PostMapping("/generate")
    @PreAuthorize("hasRole('ADMIN_RH')")
    public ApiResponse<PayslipResponse> generate(@Valid @RequestBody PayslipGenerateRequest request, Principal principal) {
        return ApiResponse.success("Payslip generated successfully", payslipService.generate(request, principal.getName()));
    }

    @PostMapping("/generate-bulk")
    @PreAuthorize("hasRole('ADMIN_RH')")
    public ApiResponse<PayslipGenerationSummaryResponse> generateBulk(@Valid @RequestBody PayslipBulkGenerateRequest request, Principal principal) {
        return ApiResponse.success("Payslips generated successfully", payslipService.generateBulk(request, principal.getName()));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN_RH')")
    public ApiResponse<List<PayslipResponse>> findAll(@Valid @ModelAttribute PayslipFilterRequest filter) {
        return ApiResponse.success("Payslips loaded successfully", payslipService.findAll(filter));
    }

    @GetMapping("/summary")
    @PreAuthorize("hasRole('ADMIN_RH')")
    public ApiResponse<PayslipSummaryResponse> summary(@RequestParam @NotNull @Min(1) @Max(12) Integer month, @RequestParam @NotNull @Min(2020) @Max(2100) Integer year) {
        return ApiResponse.success("Payslip summary loaded successfully", payslipService.summary(month, year));
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('EMPLOYE')")
    public ApiResponse<List<PayslipResponse>> mine(@RequestParam(required = false) Integer year, Principal principal) {
        return ApiResponse.success("Payslips loaded successfully", payslipService.findMine(principal.getName(), year));
    }

    @GetMapping("/me/{id}")
    @PreAuthorize("hasRole('EMPLOYE')")
    public ApiResponse<PayslipResponse> mineById(@PathVariable Long id, Principal principal) {
        return ApiResponse.success("Payslip loaded successfully", payslipService.findMineById(principal.getName(), id));
    }

    @GetMapping("/me/{id}/download")
    @PreAuthorize("hasRole('EMPLOYE')")
    public ResponseEntity<Resource> downloadMine(@PathVariable Long id, Principal principal) {
        return pdfResponse(payslipService.downloadMine(principal.getName(), id));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN_RH')")
    public ApiResponse<PayslipResponse> findById(@PathVariable Long id) {
        return ApiResponse.success("Payslip loaded successfully", payslipService.findById(id));
    }

    @GetMapping("/{id}/download")
    @PreAuthorize("hasRole('ADMIN_RH')")
    public ResponseEntity<Resource> download(@PathVariable Long id) {
        return pdfResponse(payslipService.download(id));
    }

    @PatchMapping("/{id}/cancel")
    @PreAuthorize("hasRole('ADMIN_RH')")
    public ApiResponse<PayslipResponse> cancel(@PathVariable Long id) {
        return ApiResponse.success("Payslip cancelled successfully", payslipService.cancel(id));
    }

    private ResponseEntity<Resource> pdfResponse(Resource resource) {
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getFilename() + "\"")
                .body(resource);
    }
}
