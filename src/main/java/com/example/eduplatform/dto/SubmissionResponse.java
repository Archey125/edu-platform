package com.example.eduplatform.dto;

import com.example.eduplatform.entity.enums.SubmissionStatus;

import java.time.LocalDateTime;

// решение задания
public record SubmissionResponse(
        Long id,
        Long assignmentId,
        String assignmentTitle,
        Long studentId,
        String studentName,
        String content,
        String fileUrl,
        LocalDateTime submittedAt,
        Integer score,
        String feedback,
        SubmissionStatus status
) {
}
