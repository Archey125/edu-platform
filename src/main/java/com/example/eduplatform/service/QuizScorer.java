package com.example.eduplatform.service;

import com.example.eduplatform.entity.AnswerOption;
import com.example.eduplatform.entity.Question;
import com.example.eduplatform.entity.enums.QuestionType;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

// чистая логика подсчета результата теста, без spring и бд, покрыта юнит-тестами.
// single choice зачтен если выбран ровно один вариант и он правильный,
// multiple - только если выбраны все правильные и ни одного лишнего,
// балл это сумма points зачтенных вопросов
public final class QuizScorer {

    private QuizScorer() {
    }

    // результат по одному вопросу
    public record QuestionResult(Long questionId, boolean correct) {
    }

    // итог попытки: балл, максимум и разбивка по вопросам
    public record ScoreResult(int score, int maxScore, List<QuestionResult> results) {
    }

    public static ScoreResult calculate(Collection<Question> questions, Map<Long, List<Long>> answers) {
        int score = 0;
        int maxScore = 0;
        List<QuestionResult> results = new ArrayList<>();
        for (Question question : questions) {
            int points = question.getPoints() == null ? 1 : question.getPoints();
            maxScore += points;
            Set<Long> correctIds = question.getOptions().stream()
                    .filter(AnswerOption::isCorrect)
                    .map(AnswerOption::getId)
                    .collect(Collectors.toSet());
            List<Long> selected = answers.getOrDefault(question.getId(), List.of());
            boolean correct;
            if (question.getType() == QuestionType.SINGLE_CHOICE) {
                correct = selected.size() == 1 && correctIds.contains(selected.get(0));
            } else {
                Set<Long> selectedSet = new HashSet<>(selected);
                correct = !selectedSet.isEmpty() && selectedSet.equals(correctIds);
            }
            if (correct) {
                score += points;
            }
            results.add(new QuestionResult(question.getId(), correct));
        }
        return new ScoreResult(score, maxScore, results);
    }
}
