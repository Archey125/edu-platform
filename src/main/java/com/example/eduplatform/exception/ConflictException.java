package com.example.eduplatform.exception;

// конфликт состояния -> 409 (например удаление преподавателя у которого есть курсы)
public class ConflictException extends RuntimeException {

    public ConflictException(String message) {
        super(message);
    }
}
