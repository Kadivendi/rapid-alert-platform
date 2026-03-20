package com.rapidalert.sender.dto.kafka;

import com.rapidalert.sender.dto.response.TemplateHistoryResponse;
import com.rapidalert.sender.model.NotificationStatus;
import com.rapidalert.sender.model.NotificationType;

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
