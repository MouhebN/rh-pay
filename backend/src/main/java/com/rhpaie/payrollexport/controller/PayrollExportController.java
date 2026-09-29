package com.rhpaie.payrollexport.controller;

import com.rhpaie.common.response.ApiResponse;
import com.rhpaie.payrollexport.dto.PayrollExportPreviewResponse;
import com.rhpaie.payrollexport.dto.PayrollExportSummaryResponse;
import com.rhpaie.payrollexport.service.PayrollExportService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Validated
@RestController
@RequestMapping("/api/payroll-exports")
@RequiredArgsConstructor
public class PayrollExportController {

    private static final MediaType XLSX = MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

    private final PayrollExportService payrollExportService;

    @GetMapping("/summary")
    @PreAuthorize("hasRole('ADMIN_RH')")
    public ApiResponse<PayrollExportSummaryResponse> summary(@RequestParam @NotNull @Min(1) @Max(12) Integer month,
                                                              @RequestParam @NotNull @Min(2020) @Max(2100) Integer year) {
        return ApiResponse.success("Payroll export summary loaded successfully", payrollExportService.summary(month, year));
    }

    @GetMapping("/preview")
    @PreAuthorize("hasRole('ADMIN_RH')")
    public ApiResponse<PayrollExportPreviewResponse> preview(@RequestParam @NotNull @Min(1) @Max(12) Integer month,
                                                             @RequestParam @NotNull @Min(2020) @Max(2100) Integer year) {
        return ApiResponse.success("Payroll export preview loaded successfully", payrollExportService.preview(month, year));
    }

    @GetMapping("/journal")
    @PreAuthorize("hasRole('ADMIN_RH')")
    public ResponseEntity<byte[]> journal(@RequestParam @NotNull @Min(1) @Max(12) Integer month,
                                          @RequestParam @NotNull @Min(2020) @Max(2100) Integer year) {
        return file(payrollExportService.payrollJournal(month, year), XLSX, "journal_paie_%d_%02d.xlsx".formatted(year, month));
    }

    @GetMapping("/bank-transfer")
    @PreAuthorize("hasRole('ADMIN_RH')")
    public ResponseEntity<byte[]> bankTransfer(@RequestParam @NotNull @Min(1) @Max(12) Integer month,
                                               @RequestParam @NotNull @Min(2020) @Max(2100) Integer year) {
        return file(payrollExportService.bankTransferExcel(month, year), XLSX, "virements_paie_%d_%02d.xlsx".formatted(year, month));
    }

    @GetMapping("/bank-transfer.csv")
    @PreAuthorize("hasRole('ADMIN_RH')")
    public ResponseEntity<byte[]> bankTransferCsv(@RequestParam @NotNull @Min(1) @Max(12) Integer month,
                                                  @RequestParam @NotNull @Min(2020) @Max(2100) Integer year) {
        return file(payrollExportService.bankTransferCsv(month, year), MediaType.parseMediaType("text/csv; charset=UTF-8"), "virements_paie_%d_%02d.csv".formatted(year, month));
    }

    private ResponseEntity<byte[]> file(byte[] content, MediaType mediaType, String fileName) {
        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                .body(content);
    }
}
