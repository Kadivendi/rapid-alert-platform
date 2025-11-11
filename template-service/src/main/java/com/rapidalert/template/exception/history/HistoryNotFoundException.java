package com.rapidalert.template.exception.history;

import jakarta.persistence.EntityNotFoundException;

public class HistoryNotFoundException extends EntityNotFoundException {

    public HistoryNotFoundException(String message) {
        super(message);
    }
}
