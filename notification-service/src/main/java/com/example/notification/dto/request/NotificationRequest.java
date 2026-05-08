package com.rapidalert.notification.dto.request;

import com.rapidalert.notification.dto.response.TemplateHistoryResponse;
import com.rapidalert.notification.model.NotificationType;
import lombok.Builder;

@Builder
public record NotificationRequest(
        NotificationType type,
        String credential,
        TemplateHistoryResponse template,
        Long recipientId,
        Long clientId,
        Long urlId
) {
}
