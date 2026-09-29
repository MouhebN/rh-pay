package com.rhpaie.payslip.dto;

import lombok.Builder;

@Builder
public record PayslipGenerationSummaryResponse(
        Integer generatedCount,
        Integer regeneratedCount,
        Integer skippedCount
) {
}
