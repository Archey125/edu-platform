package com.example.eduplatform.dto;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

// частичное обновление курса
public record UpdateCourseRequest(
        @Size(max = 200, message = "Название не должно превышать 200 символов")
        String title,

        @Size(max = 2000, message = "Описание не должно превышать 2000 символов")
        String description,

        Long categoryId,

        Long teacherId,

        @Positive(message = "Длительность должна быть положительной")
        Integer durationHours,

        LocalDate startDate
) {
}
