package com.rhpaie.chat.dto;

import com.rhpaie.leave.entity.LeaveType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record ChatLeaveActionRequest(
        @NotNull LeaveType type,
        @NotNull LocalDate startDate,
        @NotNull LocalDate endDate,
        @Size(max = 1000) String reason,
        String proposalId
) {
}
