package com.rapidalert.notification.dto.response;

import com.rapidalert.notification.model.NotificationStatus;
import com.rapidalert.notification.model.NotificationType;

import java.time.LocalDateTime;

public record NotificationHistoryResponse(
        Long id,
        NotificationType type,
        String credential,
        NotificationStatus status,
        Integer retryAttempts,
        LocalDateTime createdAt,
        LocalDateTime executedAt,
        TemplateHistoryResponse template,
        Long clientId
) {
}
