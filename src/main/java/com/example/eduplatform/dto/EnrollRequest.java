package com.example.eduplatform.dto;

import jakarta.validation.constraints.NotNull;

// запись на курс, userId - кто записывается
public record EnrollRequest(
        @NotNull(message = "Идентификатор пользователя обязателен")
        Long userId
) {
}
