package com.rhpaie.leave.dto;

import com.rhpaie.leave.entity.LeaveType;
import lombok.Builder;

@Builder
public record LeaveTypeStatResponse(
        LeaveType type,
        Long count,
        Double totalDays
) {
}
