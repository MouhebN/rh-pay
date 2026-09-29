package com.rhpaie.leave.dto;

import lombok.Builder;

@Builder
public record DepartmentLeaveStatResponse(
        String departmentName,
        Long count,
        Double totalDays
) {
}
