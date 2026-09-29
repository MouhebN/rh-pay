package com.rhpaie.chat.faq.dto;

import com.rhpaie.chat.faq.entity.FaqCategory;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record ChatFaqDocumentResponse(
        Long id,
        String title,
        FaqCategory category,
        String fileName,
        String contentType,
        String uploadedByUsername,
        LocalDateTime createdAt
) {
}
