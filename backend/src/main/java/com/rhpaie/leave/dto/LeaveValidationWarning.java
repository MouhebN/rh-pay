package com.rhpaie.leave.dto;

import lombok.Builder;

@Builder
public record LeaveValidationWarning(
        String message
) {
}
