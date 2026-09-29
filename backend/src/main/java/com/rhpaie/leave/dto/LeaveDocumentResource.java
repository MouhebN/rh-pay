package com.rhpaie.leave.dto;

import org.springframework.core.io.Resource;

public record LeaveDocumentResource(
        Resource resource,
        String fileName,
        String contentType,
        Long size
) {
}
