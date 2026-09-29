package com.rhpaie.attendance.mapper;

import com.rhpaie.attendance.dto.AttendanceResponse;
import com.rhpaie.attendance.entity.AttendanceRecord;
import com.rhpaie.employee.entity.Department;
import com.rhpaie.employee.entity.Employee;
import com.rhpaie.employee.entity.JobPosition;
import com.rhpaie.user.entity.UserAccount;
import org.springframework.stereotype.Component;

@Component
public class AttendanceMapper {

    public AttendanceResponse toResponse(AttendanceRecord record) {
        Employee employee = record.getEmployee();
        Department department = employee.getDepartment();
        JobPosition jobPosition = employee.getJobPosition();
        UserAccount adjustedBy = record.getAdjustedBy();

        return AttendanceResponse.builder()
                .id(record.getId())
                .employeeId(employee.getId())
                .employeeFullName(employee.getFirstName() + " " + employee.getLastName())
                .departmentName(department == null ? null : department.getName())
                .jobPositionName(jobPosition == null ? null : jobPosition.getTitle())
                .workDate(record.getWorkDate())
                .checkInTime(record.getCheckInTime())
                .checkOutTime(record.getCheckOutTime())
                .workedHours(record.getWorkedHours() == null ? null : record.getWorkedHours().doubleValue())
                .lateMinutes(record.getLateMinutes())
                .overtimeMinutes(record.getOvertimeMinutes())
                .status(record.getStatus())
                .source(record.getSource())
                .adminNote(record.getAdminNote())
                .adjustedById(adjustedBy == null ? null : adjustedBy.getId())
                .adjustedByUsername(adjustedBy == null ? null : adjustedBy.getUsername())
                .adjustedAt(record.getAdjustedAt())
                .createdAt(record.getCreatedAt())
                .updatedAt(record.getUpdatedAt())
                .build();
    }
}
