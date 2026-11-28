package com.example.eduplatform.dto;

import java.time.LocalDateTime;
import java.util.List;

// результат прохождения теста
public record QuizSubmissionResponse(
        Long id,
        Long quizId,
        Long studentId,
        Integer score,
        Integer maxScore,
        Integer percent,
        boolean passed,
        LocalDateTime takenAt
) {
}
