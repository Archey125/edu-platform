package com.example.eduplatform.dto;

import java.time.LocalDateTime;

// задание
public record AssignmentResponse(
        Long id,
        Long lessonId,
        String title,
        String description,
        LocalDateTime dueDate,
        Integer maxScore
) {
}
