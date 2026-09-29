package com.rhpaie.attendance.controller;

import com.rhpaie.attendance.dto.AttendanceAdjustRequest;
import com.rhpaie.attendance.dto.AttendanceFilterRequest;
import com.rhpaie.attendance.dto.AttendanceResponse;
import com.rhpaie.attendance.dto.AttendanceSummaryResponse;
import com.rhpaie.attendance.dto.TodayAttendanceResponse;
import com.rhpaie.attendance.service.AttendanceService;
import com.rhpaie.common.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/attendance")
@RequiredArgsConstructor
public class AttendanceController {

    private final AttendanceService attendanceService;

    @PostMapping("/check-in")
    @PreAuthorize("hasRole('EMPLOYE')")
    public ApiResponse<AttendanceResponse> checkIn(Principal principal) {
        return ApiResponse.success("Check-in recorded successfully", attendanceService.checkIn(principal.getName()));
    }

    @PostMapping("/check-out")
    @PreAuthorize("hasRole('EMPLOYE')")
    public ApiResponse<AttendanceResponse> checkOut(Principal principal) {
        return ApiResponse.success("Check-out recorded successfully", attendanceService.checkOut(principal.getName()));
    }

    @GetMapping("/today")
    @PreAuthorize("hasRole('EMPLOYE')")
    public ApiResponse<TodayAttendanceResponse> today(Principal principal) {
        return ApiResponse.success("Today attendance loaded successfully", attendanceService.today(principal.getName()));
    }

    @GetMapping("/my-records")
    @PreAuthorize("hasRole('EMPLOYE')")
    public ApiResponse<List<AttendanceResponse>> myRecords(@ModelAttribute AttendanceFilterRequest filter, Principal principal) {
        return ApiResponse.success("Attendance records loaded successfully", attendanceService.myRecords(principal.getName(), filter));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN_RH')")
    public ApiResponse<List<AttendanceResponse>> findAll(@ModelAttribute AttendanceFilterRequest filter) {
        return ApiResponse.success("Attendance records loaded successfully", attendanceService.findAll(filter));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN_RH')")
    public ApiResponse<AttendanceResponse> findById(@PathVariable Long id) {
        return ApiResponse.success("Attendance record loaded successfully", attendanceService.findById(id));
    }

    @GetMapping("/employee/{employeeId}")
    @PreAuthorize("hasRole('ADMIN_RH')")
    public ApiResponse<List<AttendanceResponse>> findByEmployee(@PathVariable Long employeeId) {
        return ApiResponse.success("Employee attendance records loaded successfully", attendanceService.findByEmployee(employeeId));
    }

    @PatchMapping("/{id}/adjust")
    @PreAuthorize("hasRole('ADMIN_RH')")
    public ApiResponse<AttendanceResponse> adjust(
            @PathVariable Long id,
            @Valid @RequestBody AttendanceAdjustRequest request,
            Principal principal
    ) {
        return ApiResponse.success("Attendance record adjusted successfully", attendanceService.adjust(id, request, principal.getName()));
    }

    @GetMapping("/summary")
    @PreAuthorize("hasRole('ADMIN_RH')")
    public ApiResponse<AttendanceSummaryResponse> summary() {
        return ApiResponse.success("Attendance summary loaded successfully", attendanceService.summary());
    }

    @PostMapping("/generate-daily-absences")
    @PreAuthorize("hasRole('ADMIN_RH')")
    public ApiResponse<Void> generateDailyAbsences(@RequestParam(required = false) LocalDate date) {
        attendanceService.generateDailyAbsences(date);
        return ApiResponse.success("Daily absences generated successfully");
    }
}
