package com.rhpaie.employeeexit.dto;

import org.springframework.core.io.Resource;

public record EmployeeExitDocumentResource(
        Resource resource,
        String fileName,
        String contentType,
        Long size
) {
}
