package com.rhpaie.contract.dto;

import lombok.Builder;

@Builder
public record ContractMonitoringResponse(
        Long activeContractsCount,
        Long expiringSoonCount,
        Long expiredContractsCount,
        Long missingDocumentCount,
        Long actionRequiredCount
) {
}
