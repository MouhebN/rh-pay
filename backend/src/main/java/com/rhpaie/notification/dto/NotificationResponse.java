package com.rhpaie.notification.dto;

import com.rhpaie.notification.entity.NotificationType;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record NotificationResponse(
        Long id,
        String title,
        String message,
        NotificationType type,
        String link,
        boolean read,
        LocalDateTime createdAt
) {
}
