package com.example.eduplatform.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

// создание отзыва
public record ReviewRequest(
        @NotNull(message = "Идентификатор студента обязателен")
        Long studentId,

        @NotNull(message = "Оценка обязательна")
        @Min(value = 1, message = "Оценка должна быть не меньше 1")
        @Max(value = 5, message = "Оценка должна быть не больше 5")
        Integer rating,

        @Size(max = 1000, message = "Комментарий не должен превышать 1000 символов")
        String comment
) {
}
