package com.rhpaie.employeeexit.dto;

import com.rhpaie.employeeexit.entity.EmployeeExitDocumentType;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record EmployeeExitDocumentResponse(
        Long id,
        Long employeeExitId,
        EmployeeExitDocumentType type,
        String originalFileName,
        String contentType,
        Long fileSize,
        Boolean visibleToEmployee,
        String uploadedByUsername,
        LocalDateTime uploadedAt
) {
}
