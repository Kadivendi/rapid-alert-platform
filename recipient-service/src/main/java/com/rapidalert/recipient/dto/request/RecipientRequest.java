package com.rapidalert.recipient.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;

@Builder
public record RecipientRequest(
        @Size(max = 50, message = "{recipient.name.size}")
        String name,

        @NotNull(message = "{recipient.email.not_null}") @Email(message = "{recipient.email.invalid}") @Size(max = 255, message = "{recipient.email.size}")
        String email,

        @Size(max = 20, message = "{recipient.phone.size}")
        String phoneNumber,

        @Size(max = 20, message = "{recipient.telegram.size}")
        String telegramId,

        /**
         * FCM device registration token. Optional — recipients without a
         * mobile install simply skip the push channel.
         */
        @Size(max = 4096, message = "{recipient.fcm_token.size}")
        String fcmToken,

        @Valid
        GeolocationRequest geolocation
) {
}
