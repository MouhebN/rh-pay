package com.rhpaie.employeeexit.controller;

import com.rhpaie.common.response.ApiResponse;
import com.rhpaie.employeeexit.dto.*;
import com.rhpaie.employeeexit.entity.EmployeeExitDocumentType;
import com.rhpaie.employeeexit.service.EmployeeExitDocumentService;
import com.rhpaie.employeeexit.service.EmployeeExitService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.security.Principal;
import java.util.List;

@Validated
@RestController
@RequestMapping("/api/employee-exits")
@RequiredArgsConstructor
public class EmployeeExitController {

    private final EmployeeExitService employeeExitService;
    private final EmployeeExitDocumentService documentService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN_RH')")
    public ApiResponse<EmployeeExitResponse> create(@Valid @RequestBody EmployeeExitCreateRequest request, Principal principal) {
        return ApiResponse.success("Employee exit created successfully", employeeExitService.create(request, principal.getName()));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN_RH')")
    public ApiResponse<List<EmployeeExitResponse>> findAll(@Valid @ModelAttribute EmployeeExitFilterRequest filter) {
        return ApiResponse.success("Employee exits loaded successfully", employeeExitService.findAll(filter));
    }

    @GetMapping("/summary")
    @PreAuthorize("hasRole('ADMIN_RH')")
    public ApiResponse<EmployeeExitSummaryResponse> summary() {
        return ApiResponse.success("Employee exit summary loaded successfully", employeeExitService.summary());
    }

    @GetMapping("/scheduled")
    @PreAuthorize("hasRole('ADMIN_RH')")
    public ApiResponse<List<EmployeeExitResponse>> scheduled() {
        return ApiResponse.success("Scheduled employee exits loaded successfully", employeeExitService.scheduled());
    }

    @GetMapping("/upcoming")
    @PreAuthorize("hasRole('ADMIN_RH')")
    public ApiResponse<List<EmployeeExitResponse>> upcoming(@RequestParam(defaultValue = "30") @Min(1) @Max(365) Integer days) {
        return ApiResponse.success("Upcoming employee exits loaded successfully", employeeExitService.upcoming(days));
    }

    @PostMapping("/process-due")
    @PreAuthorize("hasRole('ADMIN_RH')")
    public ApiResponse<Integer> processDue(Principal principal) {
        return ApiResponse.success("Due employee exits processed successfully", employeeExitService.processDue(principal.getName()));
    }

    @GetMapping("/employee/{employeeId}")
    @PreAuthorize("hasRole('ADMIN_RH')")
    public ApiResponse<List<EmployeeExitResponse>> findByEmployee(@PathVariable Long employeeId) {
        return ApiResponse.success("Employee exit history loaded successfully", employeeExitService.findByEmployee(employeeId));
    }

    @GetMapping("/me/documents")
    @PreAuthorize("hasRole('EMPLOYE')")
    public ApiResponse<List<EmployeeExitDocumentResponse>> myVisibleDocuments(Principal principal) {
        return ApiResponse.success("Employee exit documents loaded successfully", documentService.findVisibleMine(principal.getName()));
    }

    @GetMapping("/me/documents/{documentId}/download")
    @PreAuthorize("hasRole('EMPLOYE')")
    public ResponseEntity<Resource> downloadMyVisibleDocument(@PathVariable Long documentId, Principal principal) {
        return documentResponse(documentService.downloadVisibleMine(principal.getName(), documentId));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN_RH')")
    public ApiResponse<EmployeeExitResponse> findById(@PathVariable Long id) {
        return ApiResponse.success("Employee exit loaded successfully", employeeExitService.findById(id));
    }

    @PostMapping(value = "/{exitId}/documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN_RH')")
    public ApiResponse<EmployeeExitDocumentResponse> uploadDocument(
            @PathVariable Long exitId,
            @RequestParam("file") MultipartFile file,
            @RequestParam EmployeeExitDocumentType type,
            @RequestParam(defaultValue = "false") Boolean visibleToEmployee,
            Principal principal
    ) {
        return ApiResponse.success("Employee exit document uploaded successfully", documentService.upload(exitId, file, type, visibleToEmployee, principal.getName()));
    }

    @GetMapping("/{exitId}/documents")
    @PreAuthorize("hasRole('ADMIN_RH')")
    public ApiResponse<List<EmployeeExitDocumentResponse>> findDocuments(@PathVariable Long exitId) {
        return ApiResponse.success("Employee exit documents loaded successfully", documentService.findByExit(exitId));
    }

    @GetMapping("/{exitId}/documents/{documentId}/download")
    @PreAuthorize("hasRole('ADMIN_RH')")
    public ResponseEntity<Resource> downloadDocument(@PathVariable Long exitId, @PathVariable Long documentId) {
        return documentResponse(documentService.download(exitId, documentId));
    }

    @DeleteMapping("/{exitId}/documents/{documentId}")
    @PreAuthorize("hasRole('ADMIN_RH')")
    public ApiResponse<Void> deleteDocument(@PathVariable Long exitId, @PathVariable Long documentId) {
        documentService.delete(exitId, documentId);
        return ApiResponse.success("Employee exit document deleted successfully");
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN_RH')")
    public ApiResponse<EmployeeExitResponse> update(@PathVariable Long id, @Valid @RequestBody EmployeeExitUpdateRequest request) {
        return ApiResponse.success("Employee exit updated successfully", employeeExitService.update(id, request));
    }

    @PatchMapping("/{id}/complete")
    @PreAuthorize("hasRole('ADMIN_RH')")
    public ApiResponse<EmployeeExitResponse> complete(
            @PathVariable Long id,
            @RequestBody(required = false) EmployeeExitCompleteRequest request,
            Principal principal
    ) {
        return ApiResponse.success("Employee exit completed successfully", employeeExitService.complete(id, request, principal.getName()));
    }

    @PatchMapping("/{id}/cancel")
    @PreAuthorize("hasRole('ADMIN_RH')")
    public ApiResponse<EmployeeExitResponse> cancel(
            @PathVariable Long id,
            @Valid @RequestBody EmployeeExitCancelRequest request,
            Principal principal
    ) {
        return ApiResponse.success("Employee exit cancelled successfully", employeeExitService.cancel(id, request, principal.getName()));
    }

    private ResponseEntity<Resource> documentResponse(EmployeeExitDocumentResource document) {
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(document.contentType()))
                .contentLength(document.size())
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.inline()
                        .filename(document.fileName())
                        .build()
                        .toString())
                .body(document.resource());
    }
}
