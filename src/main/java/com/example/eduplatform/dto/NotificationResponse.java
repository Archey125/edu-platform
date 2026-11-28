package com.example.eduplatform.dto;

import com.example.eduplatform.entity.enums.NotificationType;

import java.time.LocalDateTime;

// уведомление
public record NotificationResponse(
        Long id,
        Long userId,
        NotificationType type,
        String message,
        LocalDateTime createdAt,
        boolean read
) {
}
