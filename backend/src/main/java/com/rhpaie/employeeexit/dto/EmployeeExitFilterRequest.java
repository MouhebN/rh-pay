package com.rhpaie.employeeexit.dto;

import com.rhpaie.employeeexit.entity.ExitStatus;
import com.rhpaie.employeeexit.entity.ExitType;

import java.time.LocalDate;

public record EmployeeExitFilterRequest(
        Long employeeId,
        Long departmentId,
        Long jobPositionId,
        ExitType type,
        ExitStatus status,
        LocalDate exitDateFrom,
        LocalDate exitDateTo
) {
}
