package com.example.eduplatform.dto;

import java.util.List;

// модуль с уроками
public record ModuleResponse(
        Long id,
        String title,
        String description,
        Integer orderIndex,
        Long quizId,
        List<LessonResponse> lessons
) {
}
