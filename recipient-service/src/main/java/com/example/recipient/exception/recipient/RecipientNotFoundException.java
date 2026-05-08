package com.rapidalert.recipient.exception.recipient;

import jakarta.persistence.EntityNotFoundException;

public class RecipientNotFoundException extends EntityNotFoundException {

    public RecipientNotFoundException(String message) {
        super(message);
    }
}
