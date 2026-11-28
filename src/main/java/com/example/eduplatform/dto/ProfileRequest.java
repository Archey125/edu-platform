package com.example.eduplatform.dto;

import jakarta.validation.constraints.Size;

import java.time.LocalDate;

// создание и редактирование профиля
public record ProfileRequest(
        @Size(max = 2000, message = "Биография не должна превышать 2000 символов")
        String bio,

        @Size(max = 500, message = "Ссылка на аватар не должна превышать 500 символов")
        String avatarUrl,

        @Size(max = 30, message = "Телефон не должен превышать 30 символов")
        String phoneNumber,

        @Size(max = 100, message = "Город не должен превышать 100 символов")
        String city,

        LocalDate birthDate
) {
}
