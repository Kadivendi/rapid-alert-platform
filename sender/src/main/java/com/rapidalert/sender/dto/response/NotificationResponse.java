package com.rapidalert.sender.dto.response;

import com.rapidalert.sender.model.NotificationStatus;
import com.rapidalert.sender.model.NotificationType;

import java.time.LocalDateTime;

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
