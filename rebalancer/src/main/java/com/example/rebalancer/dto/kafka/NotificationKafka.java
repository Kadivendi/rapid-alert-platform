package com.rapidalert.rebalancer.dto.kafka;


import com.rapidalert.rebalancer.dto.response.TemplateHistoryResponse;
import com.rapidalert.rebalancer.model.NotificationStatus;
import com.rapidalert.rebalancer.model.NotificationType;

import java.time.LocalDateTime;
import java.util.Map;

public record NotificationKafka(
        Long id,
        NotificationType type,
        String credential,
        NotificationStatus status,
        Integer retryAttempts,
        LocalDateTime createdAt,
        TemplateHistoryResponse template,
        Long clientId,
        Map<String, String> urlOptionMap
) {
}
