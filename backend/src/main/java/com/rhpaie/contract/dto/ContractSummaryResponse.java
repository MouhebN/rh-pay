package com.rhpaie.contract.dto;

import lombok.Builder;

import java.util.Map;

@Builder
public record ContractSummaryResponse(
        long totalContracts,
        long activeContracts,
        long draftContracts,
        long expiredContracts,
        long terminatedContracts,
        long expiringSoonCount,
        Map<String, Long> contractsByType
) {
}
