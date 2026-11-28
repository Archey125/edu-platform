package com.example.eduplatform.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// создание тега
public record TagRequest(
        @NotBlank(message = "Название тега не может быть пустым")
        @Size(max = 100, message = "Название не должно превышать 100 символов")
        String name
) {
}
