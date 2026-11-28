package com.example.eduplatform.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

// оценка и комментарий от преподавателя
public record GradeSubmissionRequest(
        @NotNull(message = "Оценка обязательна")
        @PositiveOrZero(message = "Оценка не может быть отрицательной")
        Integer score,

        @Size(max = 1000, message = "Комментарий не должен превышать 1000 символов")
        String feedback
) {
}
