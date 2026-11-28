package com.example.eduplatform.dto;

import com.example.eduplatform.entity.enums.QuestionType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.List;

// вопрос теста, юзается при создании теста и при добавлении вопроса
public record QuestionRequest(
        @NotBlank(message = "Текст вопроса не может быть пустым")
        @Size(max = 1000, message = "Вопрос не должен превышать 1000 символов")
        String questionText,

        @NotNull(message = "Тип вопроса обязателен")
        QuestionType type,

        @Positive(message = "Балл за вопрос должен быть положительным")
        Integer points,

        @NotEmpty(message = "Вопрос должен содержать хотя бы два варианта ответа")
        @Valid
        List<OptionRequest> options
) {
}
