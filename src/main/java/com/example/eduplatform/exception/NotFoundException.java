package com.example.eduplatform.exception;

// не найдено -> 404
public class NotFoundException extends RuntimeException {

    public NotFoundException(String message) {
        super(message);
    }
}
