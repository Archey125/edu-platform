package com.example.eduplatform.dto;

import com.example.eduplatform.entity.enums.QuestionType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.List;

// создание теста сразу с вопросами и вариантами
public record CreateQuizRequest(
        @Size(min = 1, max = 200, message = "Название должно быть от 1 до 200 символов")
        String title,

        @Positive(message = "Ограничение по времени должно быть положительным")
        Integer timeLimitMinutes,

        @NotEmpty(message = "Тест должен содержать хотя бы один вопрос")
        @Valid
        List<QuestionRequest> questions
) {
}
