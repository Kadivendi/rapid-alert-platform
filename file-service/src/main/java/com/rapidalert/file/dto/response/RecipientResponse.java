package com.rapidalert.file.dto.response;

import lombok.Builder;

@Builder
public record RecipientResponse(
        Long id,
        String name,
        String email,
        String phoneNumber,
        String telegramId,
        String fcmToken,
        GeolocationResponse geolocation
) {
}
