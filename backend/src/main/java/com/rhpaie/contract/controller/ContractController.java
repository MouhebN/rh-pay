package com.rhpaie.contract.controller;

import com.rhpaie.common.response.ApiResponse;
import com.rhpaie.contract.dto.*;
import com.rhpaie.contract.service.ContractService;
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
@RequestMapping("/api/contracts")
@RequiredArgsConstructor
public class ContractController {

    private final ContractService contractService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN_RH')")
    public ApiResponse<ContractResponse> create(@Valid @RequestBody ContractCreateRequest request, Principal principal) {
        return ApiResponse.success("Contract created successfully", contractService.create(request, principal.getName()));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN_RH')")
    public ApiResponse<List<ContractResponse>> findAll(@Valid @ModelAttribute ContractFilterRequest filter) {
        return ApiResponse.success("Contracts loaded successfully", contractService.findAll(filter));
    }

    @GetMapping("/summary")
    @PreAuthorize("hasRole('ADMIN_RH')")
    public ApiResponse<ContractSummaryResponse> summary() {
        return ApiResponse.success("Contract summary loaded successfully", contractService.summary());
    }

    @GetMapping("/monitoring")
    @PreAuthorize("hasRole('ADMIN_RH')")
    public ApiResponse<ContractMonitoringResponse> monitoring() {
        return ApiResponse.success("Contract monitoring loaded successfully", contractService.monitoring());
    }

    @GetMapping("/expiring-soon")
    @PreAuthorize("hasRole('ADMIN_RH')")
    public ApiResponse<List<ContractResponse>> expiringSoon(@RequestParam(defaultValue = "30") @Min(1) @Max(365) Integer days) {
        return ApiResponse.success("Expiring contracts loaded successfully", contractService.expiringSoon(days));
    }

    @GetMapping("/expired")
    @PreAuthorize("hasRole('ADMIN_RH')")
    public ApiResponse<List<ContractResponse>> expired() {
        return ApiResponse.success("Expired contracts loaded successfully", contractService.expired());
    }

    @GetMapping("/missing-documents")
    @PreAuthorize("hasRole('ADMIN_RH')")
    public ApiResponse<List<ContractResponse>> missingDocuments() {
        return ApiResponse.success("Contracts missing documents loaded successfully", contractService.missingDocuments());
    }

    @PostMapping("/refresh-expired")
    @PreAuthorize("hasRole('ADMIN_RH')")
    public ApiResponse<Integer> refreshExpired() {
        return ApiResponse.success("Expired contracts refreshed successfully", contractService.refreshExpired());
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('EMPLOYE')")
    public ApiResponse<List<ContractResponse>> mine(Principal principal) {
        return ApiResponse.success("Contracts loaded successfully", contractService.findMine(principal.getName()));
    }

    @GetMapping("/me/{id}")
    @PreAuthorize("hasRole('EMPLOYE')")
    public ApiResponse<ContractResponse> mineById(@PathVariable Long id, Principal principal) {
        return ApiResponse.success("Contract loaded successfully", contractService.findMineById(principal.getName(), id));
    }

    @GetMapping("/employee/{employeeId}")
    @PreAuthorize("hasRole('ADMIN_RH')")
    public ApiResponse<List<ContractResponse>> findByEmployee(@PathVariable Long employeeId) {
        return ApiResponse.success("Employee contracts loaded successfully", contractService.findByEmployee(employeeId));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN_RH')")
    public ApiResponse<ContractResponse> findById(@PathVariable Long id) {
        return ApiResponse.success("Contract loaded successfully", contractService.findById(id));
    }

    @PostMapping(value = "/{id}/document", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN_RH')")
    public ApiResponse<ContractResponse> uploadDocument(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file,
            Principal principal
    ) {
        return ApiResponse.success("Contract document uploaded successfully", contractService.uploadDocument(id, file, principal.getName()));
    }

    @GetMapping("/{id}/document")
    @PreAuthorize("hasAnyRole('ADMIN_RH', 'EMPLOYE')")
    public ResponseEntity<Resource> downloadDocument(@PathVariable Long id, Principal principal) {
        ContractDocumentResource document = contractService.downloadDocument(id, principal.getName());
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(document.contentType()))
                .contentLength(document.size())
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.inline()
                        .filename(document.fileName())
                        .build()
                        .toString())
                .body(document.resource());
    }

    @DeleteMapping("/{id}/document")
    @PreAuthorize("hasRole('ADMIN_RH')")
    public ApiResponse<ContractResponse> deleteDocument(@PathVariable Long id) {
        return ApiResponse.success("Contract document deleted successfully", contractService.deleteDocument(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN_RH')")
    public ApiResponse<ContractResponse> update(@PathVariable Long id, @Valid @RequestBody ContractUpdateRequest request, Principal principal) {
        return ApiResponse.success("Contract updated successfully", contractService.update(id, request, principal.getName()));
    }

    @PatchMapping("/{id}/activate")
    @PreAuthorize("hasRole('ADMIN_RH')")
    public ApiResponse<ContractResponse> activate(@PathVariable Long id, Principal principal) {
        return ApiResponse.success("Contract activated successfully", contractService.activate(id, principal.getName()));
    }

    @PatchMapping("/{id}/terminate")
    @PreAuthorize("hasRole('ADMIN_RH')")
    public ApiResponse<ContractResponse> terminate(@PathVariable Long id, @Valid @RequestBody ContractTerminateRequest request, Principal principal) {
        return ApiResponse.success("Contract terminated successfully", contractService.terminate(id, request, principal.getName()));
    }

    @PatchMapping("/{id}/renew")
    @PreAuthorize("hasRole('ADMIN_RH')")
    public ApiResponse<ContractResponse> renew(@PathVariable Long id, @Valid @RequestBody ContractRenewRequest request, Principal principal) {
        return ApiResponse.success("Contract renewed successfully", contractService.renew(id, request, principal.getName()));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN_RH')")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        contractService.delete(id);
        return ApiResponse.success("Contract deleted successfully");
    }
}
