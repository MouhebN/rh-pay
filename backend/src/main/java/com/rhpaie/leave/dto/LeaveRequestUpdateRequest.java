package com.rhpaie.leave.dto;

import com.rhpaie.leave.entity.LeaveType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;

import java.time.LocalDate;

@Builder
public record LeaveRequestUpdateRequest(
        @NotNull(message = "Leave type is required")
        LeaveType type,

        @NotNull(message = "Start date is required")
        LocalDate startDate,

        @NotNull(message = "End date is required")
        LocalDate endDate,

        @Size(max = 1000, message = "Reason must not exceed 1000 characters")
        String reason
) {
}
