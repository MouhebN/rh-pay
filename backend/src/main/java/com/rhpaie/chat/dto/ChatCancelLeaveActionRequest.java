package com.rhpaie.chat.dto;

import jakarta.validation.constraints.NotNull;

public record ChatCancelLeaveActionRequest(
        @NotNull Long leaveRequestId,
        String proposalId
) {
}
