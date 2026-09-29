package com.rhpaie.contract.dto;

import org.springframework.core.io.Resource;

public record ContractDocumentResource(
        Resource resource,
        String fileName,
        String contentType,
        Long size
) {
}
