package com.example.eduplatform.dto;

import java.time.LocalDateTime;

// отзыв о курсе
public record ReviewResponse(
        Long id,
        Long courseId,
        Long studentId,
        String studentName,
        Integer rating,
        String comment,
        LocalDateTime createdAt
) {
}
