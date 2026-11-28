package com.example.eduplatform.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

// студент отправляет решение
public record SubmitSolutionRequest(
        @NotNull(message = "Идентификатор студента обязателен")
        Long studentId,

        @NotBlank(message = "Текст решения не может быть пустым")
        @Size(max = 5000, message = "Решение не должно превышать 5000 символов")
        String content,

        @Size(max = 500, message = "Ссылка на файл не должна превышать 500 символов")
        String fileUrl
) {
}
