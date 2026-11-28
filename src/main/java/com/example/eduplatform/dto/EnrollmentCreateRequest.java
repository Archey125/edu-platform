package com.example.eduplatform.dto;

import com.example.eduplatform.entity.enums.EnrollmentStatus;
import jakarta.validation.constraints.NotNull;

// запись на курс через /api/enrollments
public record EnrollmentCreateRequest(
        @NotNull(message = "Курс обязателен")
        Long courseId,

        @NotNull(message = "Пользователь обязателен")
        Long userId
) {
}
