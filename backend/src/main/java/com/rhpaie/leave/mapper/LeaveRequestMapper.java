package com.rhpaie.leave.mapper;

import com.rhpaie.employee.entity.Department;
import com.rhpaie.employee.entity.Employee;
import com.rhpaie.employee.entity.JobPosition;
import com.rhpaie.leave.dto.LeaveCalendarEventResponse;
import com.rhpaie.leave.dto.LeaveRequestResponse;
import com.rhpaie.leave.entity.LeaveRequest;
import com.rhpaie.leave.entity.LeaveStatus;
import com.rhpaie.leave.entity.LeaveType;
import com.rhpaie.user.entity.UserAccount;
import org.springframework.stereotype.Component;

@Component
public class LeaveRequestMapper {

    public LeaveRequestResponse toResponse(LeaveRequest leaveRequest) {
        Employee employee = leaveRequest.getEmployee();
        Department department = employee.getDepartment();
        JobPosition jobPosition = employee.getJobPosition();
        UserAccount reviewedBy = leaveRequest.getReviewedBy();

        return LeaveRequestResponse.builder()
                .id(leaveRequest.getId())
                .employeeId(employee.getId())
                .employeeFullName(employee.getFirstName() + " " + employee.getLastName())
                .departmentName(department == null ? null : department.getName())
                .jobPositionName(jobPosition == null ? null : jobPosition.getTitle())
                .type(leaveRequest.getType())
                .startDate(leaveRequest.getStartDate())
                .endDate(leaveRequest.getEndDate())
                .totalDays(leaveRequest.getTotalDays())
                .reason(leaveRequest.getReason())
                .status(leaveRequest.getStatus())
                .reviewedById(reviewedBy == null ? null : reviewedBy.getId())
                .reviewedByUsername(reviewedBy == null ? null : reviewedBy.getUsername())
                .reviewedAt(leaveRequest.getReviewedAt())
                .rejectionReason(leaveRequest.getRejectionReason())
                .hasDocument(leaveRequest.getDocumentStoragePath() != null)
                .documentFileName(leaveRequest.getDocumentFileName())
                .documentContentType(leaveRequest.getDocumentContentType())
                .documentSize(leaveRequest.getDocumentSize())
                .documentUploadedAt(leaveRequest.getDocumentUploadedAt())
                .createdAt(leaveRequest.getCreatedAt())
                .updatedAt(leaveRequest.getUpdatedAt())
                .build();
    }

    public LeaveCalendarEventResponse toCalendarEvent(LeaveRequest leaveRequest) {
        Employee employee = leaveRequest.getEmployee();
        Department department = employee.getDepartment();
        JobPosition jobPosition = employee.getJobPosition();
        String employeeFullName = employee.getFirstName() + " " + employee.getLastName();

        return LeaveCalendarEventResponse.builder()
                .leaveRequestId(leaveRequest.getId())
                .employeeId(employee.getId())
                .employeeFullName(employeeFullName)
                .departmentName(department == null ? null : department.getName())
                .jobPositionName(jobPosition == null ? null : jobPosition.getTitle())
                .type(leaveRequest.getType())
                .status(leaveRequest.getStatus())
                .startDate(leaveRequest.getStartDate())
                .endDate(leaveRequest.getEndDate())
                .totalDays(leaveRequest.getTotalDays())
                .title(employeeFullName + " - " + typeLabel(leaveRequest.getType()))
                .color(colorFor(leaveRequest.getStatus()))
                .build();
    }

    private String typeLabel(LeaveType type) {
        return switch (type) {
            case PAID_LEAVE -> "Congé payé";
            case SICK_LEAVE -> "Maladie";
            case UNPAID_LEAVE -> "Sans solde";
            case MATERNITY_LEAVE -> "Maternité";
            case OTHER -> "Autre";
        };
    }

    private String colorFor(LeaveStatus status) {
        return switch (status) {
            case PENDING -> "#f59e0b";
            case APPROVED -> "#16a34a";
            case REJECTED -> "#dc2626";
            case CANCELLED -> "#737373";
        };
    }
}
