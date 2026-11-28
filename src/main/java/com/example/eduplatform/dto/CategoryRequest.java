package com.example.eduplatform.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// создание и редактирование категории
public record CategoryRequest(
        @NotBlank(message = "Название категории не может быть пустым")
        @Size(max = 100, message = "Название не должно превышать 100 символов")
        String name,

        @Size(max = 500, message = "Описание не должно превышать 500 символов")
        String description
) {
}
