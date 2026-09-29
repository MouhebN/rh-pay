package com.rhpaie.leave.controller;

import com.rhpaie.common.response.ApiResponse;
import com.rhpaie.leave.dto.AdminLeaveSummaryResponse;
import com.rhpaie.leave.dto.AdminLeaveStatisticsResponse;
import com.rhpaie.leave.dto.EmployeeLeaveSummaryResponse;
import com.rhpaie.leave.dto.EmployeeLeaveStatisticsResponse;
import com.rhpaie.leave.dto.LeaveCalendarEventResponse;
import com.rhpaie.leave.dto.LeaveDocumentResource;
import com.rhpaie.leave.dto.LeaveRequestCreateRequest;
import com.rhpaie.leave.dto.LeaveRequestFilter;
import com.rhpaie.leave.dto.LeaveRequestResponse;
import com.rhpaie.leave.dto.LeaveReviewRequest;
import com.rhpaie.leave.dto.LeaveValidationResponse;
import com.rhpaie.leave.entity.LeaveStatus;
import com.rhpaie.leave.entity.LeaveType;
import com.rhpaie.leave.service.LeaveRequestService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.core.io.Resource;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.security.Principal;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/leave-requests")
@RequiredArgsConstructor
public class LeaveRequestController {

    private final LeaveRequestService leaveRequestService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN_RH', 'EMPLOYE')")
    public ApiResponse<LeaveRequestResponse> create(
            @Valid @RequestBody LeaveRequestCreateRequest request,
            Principal principal
    ) {
        return ApiResponse.success("Leave request created successfully", leaveRequestService.create(request, principal.getName()));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN_RH')")
    public ApiResponse<List<LeaveRequestResponse>> findAll(@ModelAttribute LeaveRequestFilter filter) {
        return ApiResponse.success("Leave requests loaded successfully", leaveRequestService.findAll(filter));
    }

    @GetMapping("/my-requests")
    @PreAuthorize("hasRole('EMPLOYE')")
    public ApiResponse<List<LeaveRequestResponse>> findMyRequests(Principal principal) {
        return ApiResponse.success("Leave requests loaded successfully", leaveRequestService.findMyRequests(principal.getName()));
    }

    @GetMapping("/calendar")
    @PreAuthorize("hasRole('ADMIN_RH')")
    public ApiResponse<List<LeaveCalendarEventResponse>> adminCalendar(
            @RequestParam LocalDate startDate,
            @RequestParam LocalDate endDate,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) LeaveStatus status,
            @RequestParam(required = false) LeaveType type
    ) {
        return ApiResponse.success(
                "Leave calendar loaded successfully",
                leaveRequestService.adminCalendar(startDate, endDate, departmentId, status, type)
        );
    }

    @GetMapping("/calendar/me")
    @PreAuthorize("hasRole('EMPLOYE')")
    public ApiResponse<List<LeaveCalendarEventResponse>> myCalendar(
            @RequestParam LocalDate startDate,
            @RequestParam LocalDate endDate,
            Principal principal
    ) {
        return ApiResponse.success(
                "Leave calendar loaded successfully",
                leaveRequestService.myCalendar(principal.getName(), startDate, endDate)
        );
    }

    @PostMapping(value = "/{id}/document", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN_RH', 'EMPLOYE')")
    public ApiResponse<LeaveRequestResponse> uploadDocument(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file,
            Principal principal
    ) {
        return ApiResponse.success("Document uploaded successfully", leaveRequestService.uploadDocument(id, file, principal.getName()));
    }

    @GetMapping("/{id}/document")
    @PreAuthorize("hasAnyRole('ADMIN_RH', 'EMPLOYE')")
    public ResponseEntity<Resource> downloadDocument(@PathVariable Long id, Principal principal) {
        LeaveDocumentResource document = leaveRequestService.downloadDocument(id, principal.getName());
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
    @PreAuthorize("hasAnyRole('ADMIN_RH', 'EMPLOYE')")
    public ApiResponse<LeaveRequestResponse> deleteDocument(@PathVariable Long id, Principal principal) {
        return ApiResponse.success("Document deleted successfully", leaveRequestService.deleteDocument(id, principal.getName()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN_RH', 'EMPLOYE')")
    public ApiResponse<LeaveRequestResponse> findById(@PathVariable Long id, Principal principal) {
        return ApiResponse.success("Leave request loaded successfully", leaveRequestService.findById(id, principal.getName()));
    }

    @GetMapping("/{id}/validate")
    @PreAuthorize("hasRole('ADMIN_RH')")
    public ApiResponse<LeaveValidationResponse> validate(@PathVariable Long id) {
        return ApiResponse.success("Leave request validation loaded successfully", leaveRequestService.validate(id));
    }

    @PatchMapping("/{id}/approve")
    @PreAuthorize("hasRole('ADMIN_RH')")
    public ApiResponse<LeaveRequestResponse> approve(@PathVariable Long id, Principal principal) {
        return ApiResponse.success("Leave request approved successfully", leaveRequestService.approve(id, principal.getName()));
    }

    @PatchMapping("/{id}/reject")
    @PreAuthorize("hasRole('ADMIN_RH')")
    public ApiResponse<LeaveRequestResponse> reject(
            @PathVariable Long id,
            @Valid @RequestBody LeaveReviewRequest request,
            Principal principal
    ) {
        return ApiResponse.success("Leave request rejected successfully", leaveRequestService.reject(id, request, principal.getName()));
    }

    @PatchMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('ADMIN_RH', 'EMPLOYE')")
    public ApiResponse<LeaveRequestResponse> cancel(@PathVariable Long id, Principal principal) {
        return ApiResponse.success("Leave request cancelled successfully", leaveRequestService.cancel(id, principal.getName()));
    }

    @GetMapping("/summary")
    @PreAuthorize("hasRole('ADMIN_RH')")
    public ApiResponse<AdminLeaveSummaryResponse> adminSummary() {
        return ApiResponse.success("Leave summary loaded successfully", leaveRequestService.adminSummary());
    }

    @GetMapping("/my-summary")
    @PreAuthorize("hasRole('EMPLOYE')")
    public ApiResponse<EmployeeLeaveSummaryResponse> mySummary(Principal principal) {
        return ApiResponse.success("Leave summary loaded successfully", leaveRequestService.mySummary(principal.getName()));
    }

    @GetMapping("/statistics/admin")
    @PreAuthorize("hasRole('ADMIN_RH')")
    public ApiResponse<AdminLeaveStatisticsResponse> adminStatistics(@RequestParam(required = false) Integer year) {
        return ApiResponse.success("Leave statistics loaded successfully", leaveRequestService.adminStatistics(year));
    }

    @GetMapping("/statistics/me")
    @PreAuthorize("hasRole('EMPLOYE')")
    public ApiResponse<EmployeeLeaveStatisticsResponse> myStatistics(
            @RequestParam(required = false) Integer year,
            Principal principal
    ) {
        return ApiResponse.success("Leave statistics loaded successfully", leaveRequestService.myStatistics(principal.getName(), year));
    }
}
