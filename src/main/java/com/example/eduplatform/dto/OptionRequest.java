package com.example.eduplatform.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// вариант ответа при создании вопроса
public record OptionRequest(
        @NotBlank(message = "Текст варианта не может быть пустым")
        @Size(max = 500, message = "Вариант не должен превышать 500 символов")
        String optionText,

        boolean isCorrect
) {
}
