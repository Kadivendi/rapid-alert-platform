package com.rapidalert.notification.util;

import com.rapidalert.notification.model.NotificationStatus;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class NotificationStatusConverter extends BaseEnumConverter<NotificationStatus> {

    public NotificationStatusConverter() {
        super(NotificationStatus.class);
    }
}
