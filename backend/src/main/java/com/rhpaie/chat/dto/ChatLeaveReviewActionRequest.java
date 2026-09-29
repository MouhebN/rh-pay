package com.rhpaie.chat.dto;

import lombok.Builder;

import java.util.List;

@Builder
public record ChatLeaveReviewActionRequest(
        List<Long> approveLeaveRequestIds,
        List<Long> rejectLeaveRequestIds,
        String reason,
        String proposalId
) {
}
