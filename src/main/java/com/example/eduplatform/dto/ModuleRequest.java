package com.example.eduplatform.dto;

import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

// создание и редактирование модуля
public record ModuleRequest(
        @Size(min = 1, max = 200, message = "Название должно быть от 1 до 200 символов")
        String title,

        @Size(max = 2000, message = "Описание не должно превышать 2000 символов")
        String description,

        @PositiveOrZero(message = "Порядковый номер не может быть отрицательным")
        Integer orderIndex
) {
}
