package com.example.eduplatform.dto;

import com.example.eduplatform.entity.enums.EnrollmentStatus;

import java.time.LocalDateTime;

// запись на курс
public record EnrollmentResponse(
        Long id,
        Long studentId,
        String studentName,
        Long courseId,
        String courseTitle,
        EnrollmentStatus status,
        LocalDateTime enrolledAt
) {
}
