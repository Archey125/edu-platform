package com.example.eduplatform.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

// создание курса, категория и преподаватель обязательны
public record CreateCourseRequest(
        @Size(min = 1, max = 200, message = "Название должно быть от 1 до 200 символов")
        String title,

        @Size(max = 2000, message = "Описание не должно превышать 2000 символов")
        String description,

        @NotNull(message = "Категория обязательна")
        Long categoryId,

        @NotNull(message = "Преподаватель обязателен")
        Long teacherId,

        @Positive(message = "Длительность должна быть положительной")
        Integer durationHours,

        LocalDate startDate
) {
}
