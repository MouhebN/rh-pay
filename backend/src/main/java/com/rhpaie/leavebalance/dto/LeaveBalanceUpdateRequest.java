package com.rhpaie.leavebalance.dto;

import jakarta.validation.constraints.DecimalMin;
import lombok.Builder;

@Builder
public record LeaveBalanceUpdateRequest(
        Integer year,

        @DecimalMin(value = "0.0", message = "Acquired days must be positive")
        Double acquiredDays
) {
}
