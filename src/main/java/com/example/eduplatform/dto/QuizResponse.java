package com.example.eduplatform.dto;

import com.example.eduplatform.entity.enums.QuestionType;

import java.util.List;

// тест с вопросами и вариантами, правильные ответы наружу не отдаем (проверка на сервере)
public record QuizResponse(
        Long id,
        Long moduleId,
        String title,
        Integer timeLimitMinutes,
        List<QuestionResponse> questions
) {
}
