package com.example.eduplatform.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

// курс целиком: модули, внутри них уроки и задания
public record CourseDetailResponse(
        Long id,
        String title,
        String description,
        Long categoryId,
        String categoryName,
        Long teacherId,
        String teacherName,
        Integer durationHours,
        LocalDate startDate,
        LocalDateTime createdAt,
        List<String> tags,
        List<ModuleResponse> modules
) {
}
