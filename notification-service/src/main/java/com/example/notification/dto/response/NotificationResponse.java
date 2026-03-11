package com.rapidalert.notification.dto.response;

import com.rapidalert.notification.model.NotificationStatus;
import com.rapidalert.notification.model.NotificationType;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record NotificationResponse(
        Long id,
        NotificationType type,
        String credential,
        NotificationStatus status,
        Integer retryAttempts,
        LocalDateTime createdAt,
        TemplateHistoryResponse template,
        Long clientId
) {
}
