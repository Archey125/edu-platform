package com.example.eduplatform.dto;

import com.example.eduplatform.entity.enums.QuestionType;

import java.util.List;

// вопрос с вариантами, без пометок о правильности
public record QuestionResponse(
        Long id,
        String questionText,
        QuestionType type,
        Integer points,
        List<OptionResponse> options
) {
}
