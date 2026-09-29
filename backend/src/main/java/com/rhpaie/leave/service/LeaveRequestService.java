package com.rhpaie.leave.service;

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
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;

public interface LeaveRequestService {

    LeaveRequestResponse create(LeaveRequestCreateRequest request, String username);

    List<LeaveRequestResponse> findAll(LeaveRequestFilter filter);

    List<LeaveRequestResponse> findMyRequests(String username);

    LeaveRequestResponse findById(Long id, String username);

    LeaveValidationResponse validate(Long id);

    LeaveRequestResponse approve(Long id, String reviewerUsername);

    LeaveRequestResponse reject(Long id, LeaveReviewRequest request, String reviewerUsername);

    LeaveRequestResponse cancel(Long id, String username);

    AdminLeaveSummaryResponse adminSummary();

    EmployeeLeaveSummaryResponse mySummary(String username);

    AdminLeaveStatisticsResponse adminStatistics(Integer year);

    EmployeeLeaveStatisticsResponse myStatistics(String username, Integer year);

    List<LeaveCalendarEventResponse> adminCalendar(
            LocalDate startDate,
            LocalDate endDate,
            Long departmentId,
            LeaveStatus status,
            LeaveType type
    );

    List<LeaveCalendarEventResponse> myCalendar(String username, LocalDate startDate, LocalDate endDate);

    LeaveRequestResponse uploadDocument(Long id, MultipartFile file, String username);

    LeaveDocumentResource downloadDocument(Long id, String username);

    LeaveRequestResponse deleteDocument(Long id, String username);
}
