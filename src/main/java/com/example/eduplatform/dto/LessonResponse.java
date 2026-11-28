package com.example.eduplatform.dto;

import java.util.List;

// урок с заданиями
public record LessonResponse(
        Long id,
        String title,
        String content,
        String videoUrl,
        Integer orderIndex,
        List<AssignmentResponse> assignments
) {
}
