package com.example.eduplatform.dto;

import com.example.eduplatform.entity.enums.UserRole;

import java.time.LocalDateTime;

// пользователь + его профиль
public record UserDetailResponse(
        Long id,
        String name,
        String email,
        UserRole role,
        boolean active,
        LocalDateTime createdAt,
        ProfileResponse profile
) {
}
