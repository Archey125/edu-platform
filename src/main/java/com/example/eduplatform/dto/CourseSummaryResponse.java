package com.example.eduplatform.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

// краткая инфа о курсе, без модулей
public record CourseSummaryResponse(
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
        List<String> tags
) {
}
