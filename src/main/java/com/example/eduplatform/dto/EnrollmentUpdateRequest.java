package com.example.eduplatform.dto;

import com.example.eduplatform.entity.enums.EnrollmentStatus;
import jakarta.validation.constraints.NotNull;

// смена статуса записи
public record EnrollmentUpdateRequest(
        @NotNull(message = "Статус обязателен")
        EnrollmentStatus status
) {
}
