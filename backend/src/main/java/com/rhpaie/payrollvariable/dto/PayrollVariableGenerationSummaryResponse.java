package com.rhpaie.payrollvariable.dto;

import lombok.Builder;

@Builder
public record PayrollVariableGenerationSummaryResponse(
        Integer generatedCount,
        Integer skippedValidatedCount,
        Integer updatedDraftCount
) {
}
