package com.rhpaie.leave.dto;

import jakarta.validation.constraints.Size;
import lombok.Builder;

@Builder
public record LeaveReviewRequest(
        @Size(max = 1000, message = "Rejection reason must not exceed 1000 characters")
        String rejectionReason
) {
}
