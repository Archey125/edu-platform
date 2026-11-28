package com.example.eduplatform.dto;

import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

// создание и редактирование урока
public record LessonRequest(
        @Size(min = 1, max = 200, message = "Название должно быть от 1 до 200 символов")
        String title,

        @Size(max = 5000, message = "Содержимое не должно превышать 5000 символов")
        String content,

        @Size(max = 500, message = "Ссылка на видео не должна превышать 500 символов")
        String videoUrl,

        @PositiveOrZero(message = "Порядковый номер не может быть отрицательным")
        Integer orderIndex
) {
}
