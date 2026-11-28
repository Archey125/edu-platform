package com.example.eduplatform.dto;

import java.time.LocalDate;

// профиль
public record ProfileResponse(
        Long id,
        Long userId,
        String bio,
        String avatarUrl,
        String phoneNumber,
        String city,
        LocalDate birthDate
) {
}
