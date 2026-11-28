package com.example.eduplatform.dto;

import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.Map;

// прохождение теста
// answers - мапа id вопроса -> список id выбранных вариантов, для single choice ровно один
public record TakeQuizRequest(
        @NotNull(message = "Идентификатор студента обязателен")
        Long studentId,

        @NotNull(message = "Ответы обязательны")
        Map<Long, List<Long>> answers
) {
}
