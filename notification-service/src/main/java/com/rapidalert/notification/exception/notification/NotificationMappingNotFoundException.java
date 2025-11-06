package com.rapidalert.notification.exception.notification;

import jakarta.persistence.EntityNotFoundException;

public class NotificationMappingNotFoundException extends EntityNotFoundException {

    public NotificationMappingNotFoundException(String message) {
        super(message);
    }
}
