package com.rhpaie.chat.dto;

import lombok.Builder;

import java.time.LocalDate;
import java.util.List;

@Builder
public record ChatProposedAction(
        String type,
        String leaveType,
        LocalDate startDate,
        LocalDate endDate,
        String reason,
        Long leaveRequestId,
        List<Long> approveLeaveRequestIds,
        List<Long> rejectLeaveRequestIds,
        String proposalId
) {
}
