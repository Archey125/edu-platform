package com.example.eduplatform.unit;

import com.example.eduplatform.entity.AnswerOption;
import com.example.eduplatform.entity.Question;
import com.example.eduplatform.entity.Quiz;
import com.example.eduplatform.entity.enums.QuestionType;
import com.example.eduplatform.service.QuizScorer;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

// юнит-тесты логики подсчета балла, без spring и базы
class QuizScorerTest {

    private Quiz quizWithSingleQuestion(int points) {
        Question question = new Question();
        question.setId(1L);
        question.setPoints(points);
        question.setType(QuestionType.SINGLE_CHOICE);
        AnswerOption correct = new AnswerOption("Верный", true);
        correct.setId(10L);
        AnswerOption wrong = new AnswerOption("Неверный", false);
        wrong.setId(11L);
        question.addOption(correct);
        question.addOption(wrong);
        Quiz quiz = new Quiz();
        quiz.setId(100L);
        quiz.addQuestion(question);
        return quiz;
    }

    @Test
    void singleChoiceCorrectAnswerScoresFullPoints() {
        Quiz quiz = quizWithSingleQuestion(5);
        QuizScorer.ScoreResult result = QuizScorer.calculate(quiz.getQuestions(), Map.of(1L, List.of(10L)));
        assertThat(result.score()).isEqualTo(5);
        assertThat(result.maxScore()).isEqualTo(5);
        assertThat(result.results()).hasSize(1);
        assertThat(result.results().get(0).correct()).isTrue();
    }

    @Test
    void singleChoiceWrongAnswerScoresZero() {
        Quiz quiz = quizWithSingleQuestion(5);
        QuizScorer.ScoreResult result = QuizScorer.calculate(quiz.getQuestions(), Map.of(1L, List.of(11L)));
        assertThat(result.score()).isZero();
        assertThat(result.maxScore()).isEqualTo(5);
    }

    @Test
    void multipleChoiceFullCorrectSetScoresPoints() {
        Quiz quiz = quizWithMultipleQuestion(3);
        QuizScorer.ScoreResult result = QuizScorer.calculate(quiz.getQuestions(), Map.of(1L, List.of(10L, 12L)));
        assertThat(result.score()).isEqualTo(3);
        assertThat(result.maxScore()).isEqualTo(3);
    }

    @Test
    void multipleChoiceWithOneWrongSelectionScoresZero() {
        Quiz quiz = quizWithMultipleQuestion(3);
        QuizScorer.ScoreResult result = QuizScorer.calculate(quiz.getQuestions(), Map.of(1L, List.of(10L, 11L, 12L)));
        assertThat(result.score()).isZero();
    }

    @Test
    void multipleChoiceMissingOneCorrectScoresZero() {
        Quiz quiz = quizWithMultipleQuestion(3);
        QuizScorer.ScoreResult result = QuizScorer.calculate(quiz.getQuestions(), Map.of(1L, List.of(10L)));
        assertThat(result.score()).isZero();
        assertThat(result.maxScore()).isEqualTo(3);
    }

    @Test
    void maxScoreIsSumOfQuestionPoints() {
        Question single = new Question();
        single.setId(1L);
        single.setPoints(2);
        single.setType(QuestionType.SINGLE_CHOICE);
        AnswerOption s1 = new AnswerOption("Верный", true);
        s1.setId(10L);
        single.addOption(s1);

        Question multiple = new Question();
        multiple.setId(2L);
        multiple.setPoints(3);
        multiple.setType(QuestionType.MULTIPLE_CHOICE);
        AnswerOption m1 = new AnswerOption("Верный 1", true);
        m1.setId(20L);
        AnswerOption m2 = new AnswerOption("Верный 2", true);
        m2.setId(21L);
        multiple.addOption(m1);
        multiple.addOption(m2);

        Quiz quiz = new Quiz();
        quiz.setId(100L);
        quiz.addQuestion(single);
        quiz.addQuestion(multiple);

        QuizScorer.ScoreResult result = QuizScorer.calculate(quiz.getQuestions(),
                Map.of(1L, List.of(10L), 2L, List.of(20L, 21L)));
        assertThat(result.score()).isEqualTo(5);
        assertThat(result.maxScore()).isEqualTo(5);
        assertThat(result.results()).extracting(QuizScorer.QuestionResult::correct)
                .containsExactly(true, true);
    }

    @Test
    void emptyAnswerMapGivesZeroButCountsMaxScore() {
        Quiz quiz = quizWithSingleQuestion(4);
        QuizScorer.ScoreResult result = QuizScorer.calculate(quiz.getQuestions(), Map.of());
        assertThat(result.score()).isZero();
        assertThat(result.maxScore()).isEqualTo(4);
    }

    private Quiz quizWithMultipleQuestion(int points) {
        Question question = new Question();
        question.setId(1L);
        question.setPoints(points);
        question.setType(QuestionType.MULTIPLE_CHOICE);
        AnswerOption correct1 = new AnswerOption("Верный 1", true);
        correct1.setId(10L);
        AnswerOption wrong = new AnswerOption("Неверный", false);
        wrong.setId(11L);
        AnswerOption correct2 = new AnswerOption("Верный 2", true);
        correct2.setId(12L);
        question.addOption(correct1);
        question.addOption(wrong);
        question.addOption(correct2);
        Quiz quiz = new Quiz();
        quiz.setId(100L);
        quiz.addQuestion(question);
        return quiz;
    }
}
