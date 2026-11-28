package com.example.eduplatform.dto;

import com.example.eduplatform.entity.enums.UserRole;

import java.time.LocalDateTime;

// пользователь
public record UserResponse(
        Long id,
        String name,
        String email,
        UserRole role,
        boolean active,
        LocalDateTime createdAt
) {
}
