package com.example.eduplatform.exception;

// дубликат (email, запись на курс и тп) -> 409
public class DuplicateResourceException extends RuntimeException {

    public DuplicateResourceException(String message) {
        super(message);
    }
}
