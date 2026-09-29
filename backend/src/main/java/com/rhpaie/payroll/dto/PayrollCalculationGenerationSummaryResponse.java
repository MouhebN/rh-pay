package com.rhpaie.payroll.dto;

import lombok.Builder;

@Builder
public record PayrollCalculationGenerationSummaryResponse(
        Integer generatedCount,
        Integer updatedDraftCount,
        Integer skippedFinalizedCount,
        Integer skippedInvalidVariablesCount
) {
}
