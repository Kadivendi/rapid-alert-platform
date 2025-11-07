package com.rapidalert.notification.model;

public enum NotificationType implements EnumeratedEntityField {
    EMAIL("EML"),
    PHONE("PHN"),
    TELEGRAM("TGM"),
    PUSH("PSH");

    private final String code;

    NotificationType(String code) {
        this.code = code;
    }

    @Override
    public String getCode() {
        return code;
    }
}
