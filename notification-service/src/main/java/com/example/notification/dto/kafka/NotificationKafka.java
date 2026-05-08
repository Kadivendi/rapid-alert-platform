package com.rapidalert.notification.dto.kafka;

import com.rapidalert.notification.dto.response.TemplateHistoryResponse;
import com.rapidalert.notification.model.NotificationStatus;
import com.rapidalert.notification.model.NotificationType;

import java.util.Map;

public record NotificationKafka(
        Long id,
        NotificationType type,
        String credential,
        NotificationStatus status,
        Integer retryAttempts,
        TemplateHistoryResponse template,
        Long clientId,
        Map<String, String> urlOptionMap
) {
}
