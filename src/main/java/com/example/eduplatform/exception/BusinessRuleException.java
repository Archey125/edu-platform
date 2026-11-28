package com.example.eduplatform.exception;

// нарушение бизнес-правила -> 400
public class BusinessRuleException extends RuntimeException {

    public BusinessRuleException(String message) {
        super(message);
    }
}
