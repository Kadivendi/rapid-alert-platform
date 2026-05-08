package com.rapidalert.notification.dto.kafka;

import com.rapidalert.notification.dto.response.TemplateHistoryResponse;

import java.util.List;

public record RecipientListKafka(
        List<Long> recipientIds,
        TemplateHistoryResponse templateHistoryResponse,
        Long clientId
) {
}
