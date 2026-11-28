package com.example.eduplatform.dto;

// вариант ответа, isCorrect наружу не отдаем
public record OptionResponse(
        Long id,
        String optionText
) {
}
