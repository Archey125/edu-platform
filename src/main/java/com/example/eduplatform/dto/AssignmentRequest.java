package com.example.eduplatform.dto;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

// создание и редактирование задания
public record AssignmentRequest(
        @Size(min = 1, max = 200, message = "Название должно быть от 1 до 200 символов")
        String title,

        @Size(max = 2000, message = "Описание не должно превышать 2000 символов")
        String description,

        LocalDateTime dueDate,

        @Positive(message = "Максимальный балл должен быть положительным")
        Integer maxScore
) {
}
