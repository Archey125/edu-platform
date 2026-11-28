package com.example.eduplatform.integration;

import com.example.eduplatform.dto.CreateQuizRequest;
import com.example.eduplatform.dto.OptionRequest;
import com.example.eduplatform.dto.QuestionRequest;
import com.example.eduplatform.dto.QuizResponse;
import com.example.eduplatform.dto.QuizSubmissionResponse;
import com.example.eduplatform.dto.TakeQuizRequest;
import com.example.eduplatform.entity.Course;
import com.example.eduplatform.entity.Module;
import com.example.eduplatform.entity.Question;
import com.example.eduplatform.entity.Quiz;
import com.example.eduplatform.entity.User;
import com.example.eduplatform.entity.enums.QuestionType;
import com.example.eduplatform.exception.BusinessRuleException;
import com.example.eduplatform.exception.DuplicateResourceException;
import com.example.eduplatform.exception.NotFoundException;
import com.example.eduplatform.repository.QuizSubmissionRepository;
import com.example.eduplatform.service.QuizService;
import com.example.eduplatform.support.TestDataFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// тесты: создание теста модуля, прохождение с автоподсчетом, запрет пересдачи, результаты
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class QuizServiceIntegrationTest {

    @Autowired private QuizService quizService;
    @Autowired private TestDataFactory factory;
    @Autowired private QuizSubmissionRepository quizSubmissionRepository;

    private User teacher;
    private User student;
    private Course course;
    private Module module;

    @BeforeEach
    void setUp() {
        teacher = factory.teacher();
        student = factory.student();
        course = factory.course(teacher, factory.category());
        module = factory.module(course);
        factory.enroll(student, course);
    }

    @Test
    void quizIsCreatedForModuleWithQuestions() {
        QuizResponse quiz = createStandardQuiz();

        assertThat(quiz.id()).isNotNull();
        assertThat(quiz.moduleId()).isEqualTo(module.getId());
        assertThat(quiz.questions()).hasSize(2);
        // порядок вопросов в set не гарантирован, ищу по тексту а не по позиции
        var single = quiz.questions().stream()
                .filter(q -> q.questionText().equals("Сколько будет 2+2?"))
                .findFirst()
                .orElseThrow();
        var multiple = quiz.questions().stream()
                .filter(q -> q.questionText().equals("Выберите ORM-фреймворки"))
                .findFirst()
                .orElseThrow();
        assertThat(single.options()).hasSize(2);
        assertThat(multiple.options()).hasSize(3);
    }

    @Test
    void onlyOneQuizPerModuleIsAllowed() {
        createStandardQuiz();

        assertThatThrownBy(this::createStandardQuiz)
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("уже есть тест");
    }

    @Test
    void questionWithoutCorrectOptionIsRejected() {
        assertThatThrownBy(() -> quizService.createForModule(module.getId(), new CreateQuizRequest(
                "Тест", 10,
                List.of(new QuestionRequest("Все варианты неверные", QuestionType.SINGLE_CHOICE, 1,
                        List.of(new OptionRequest("Вариант 1", false), new OptionRequest("Вариант 2", false)))))))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("правильный вариант");
    }

    @Test
    void singleChoiceWithTwoCorrectOptionsIsRejected() {
        assertThatThrownBy(() -> quizService.createForModule(module.getId(), new CreateQuizRequest(
                "Тест", 10,
                List.of(new QuestionRequest("Два правильных", QuestionType.SINGLE_CHOICE, 1,
                        List.of(new OptionRequest("Вариант 1", true), new OptionRequest("Вариант 2", true)))))))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("SINGLE_CHOICE");
    }

    @Test
    void studentTakesQuizAndScoreIsCalculated() {
        Quiz quiz = factory.quizWithMixedQuestions(module);
        // студент уже записан на курс в setUp, повторная запись сломала бы unique констрейнт
        Question single = factory.questionByText(quiz, "Вопрос с одним ответом");
        Question multiple = factory.questionByText(quiz, "Вопрос с несколькими ответами");

        var result = quizService.take(quiz.getId(), new TakeQuizRequest(student.getId(), Map.of(
                single.getId(), List.of(factory.optionByText(single, "Верный ответ А").getId()),
                multiple.getId(), List.of(factory.optionByText(multiple, "Множественный 1").getId(),
                        factory.optionByText(multiple, "Множественный 2").getId()))));

        // оба вопроса отвечены верно, 2 из 2 баллов
        assertThat(result.score()).isEqualTo(2);
        assertThat(result.maxScore()).isEqualTo(2);
        assertThat(result.percent()).isEqualTo(100);
        assertThat(result.passed()).isTrue();

        // результат сохранен в бд
        assertThat(quizSubmissionRepository.findById(result.id())).isPresent();
    }

    @Test
    void partialMultipleChoiceGivesNoPointsForThatQuestion() {
        Quiz quiz = factory.quizWithMixedQuestions(module);
        Question single = factory.questionByText(quiz, "Вопрос с одним ответом");
        Question multiple = factory.questionByText(quiz, "Вопрос с несколькими ответами");

        var result = quizService.take(quiz.getId(), new TakeQuizRequest(student.getId(), Map.of(
                single.getId(), List.of(factory.optionByText(single, "Верный ответ А").getId()),
                multiple.getId(), List.of(factory.optionByText(multiple, "Множественный 1").getId()))));

        // первый вопрос верно (1 балл), во втором выбран только 1 из 2 правильных (0)
        assertThat(result.score()).isEqualTo(1);
        assertThat(result.maxScore()).isEqualTo(2);
        assertThat(result.percent()).isEqualTo(50);
        assertThat(result.passed()).isFalse(); // 50% меньше порога 60%, незачет
    }

    @Test
    void unansweredQuestionIsRejected() {
        Quiz quiz = factory.quizWithMixedQuestions(module);
        Question single = factory.questionByText(quiz, "Вопрос с одним ответом");

        assertThatThrownBy(() -> quizService.take(quiz.getId(), new TakeQuizRequest(student.getId(),
                Map.of(single.getId(), List.of(factory.optionByText(single, "Верный ответ А").getId())))))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("остался без ответа");
    }

    @Test
    void optionFromAnotherQuestionIsRejected() {
        Quiz quiz = factory.quizWithMixedQuestions(module);
        Question single = factory.questionByText(quiz, "Вопрос с одним ответом");
        Question multiple = factory.questionByText(quiz, "Вопрос с несколькими ответами");
        Long foreignOptionId = factory.optionByText(multiple, "Множественный 1").getId();

        assertThatThrownBy(() -> quizService.take(quiz.getId(), new TakeQuizRequest(student.getId(),
                Map.of(single.getId(), List.of(foreignOptionId),
                        multiple.getId(), List.of(factory.optionByText(multiple, "Множественный 1").getId())))))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("не относится к вопросу");
    }

    @Test
    void retakeIsRejected() {
        Quiz quiz = factory.quizWithMixedQuestions(module);
        takeAllCorrect(quiz);

        Question single = factory.questionByText(quiz, "Вопрос с одним ответом");
        Question multiple = factory.questionByText(quiz, "Вопрос с несколькими ответами");
        assertThatThrownBy(() -> quizService.take(quiz.getId(), new TakeQuizRequest(student.getId(), Map.of(
                single.getId(), List.of(factory.optionByText(single, "Верный ответ А").getId()),
                multiple.getId(), List.of(factory.optionByText(multiple, "Множественный 1").getId())))))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("уже пройден");
    }

    @Test
    void notEnrolledStudentCannotTakeQuiz() {
        Quiz quiz = factory.quizWithMixedQuestions(module);
        User outsider = factory.student();

        Question single = factory.questionByText(quiz, "Вопрос с одним ответом");
        Question multiple = factory.questionByText(quiz, "Вопрос с несколькими ответами");
        assertThatThrownBy(() -> quizService.take(quiz.getId(), new TakeQuizRequest(outsider.getId(), Map.of(
                single.getId(), List.of(factory.optionByText(single, "Верный ответ А").getId()),
                multiple.getId(), List.of(factory.optionByText(multiple, "Множественный 1").getId(),
                        factory.optionByText(multiple, "Множественный 2").getId())))))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("записанным");
    }

    @Test
    void resultsAreListedByQuizAndByStudent() {
        Quiz quiz = factory.quizWithMixedQuestions(module);
        User student2 = factory.student();
        factory.enroll(student2, course);
        takeAllCorrect(quiz);

        Question single = factory.questionByText(quiz, "Вопрос с одним ответом");
        Question multiple = factory.questionByText(quiz, "Вопрос с несколькими ответами");
        quizService.take(quiz.getId(), new TakeQuizRequest(student2.getId(), Map.of(
                single.getId(), List.of(factory.optionByText(single, "Неверный ответ Б").getId()),
                multiple.getId(), List.of(factory.optionByText(multiple, "Множественный 1").getId()))));

        var all = quizService.getResults(quiz.getId(), null);
        assertThat(all).hasSize(2);
        assertThat(all).extracting(QuizSubmissionResponse::score)
                .containsExactlyInAnyOrder(2, 0);

        var onlyStudent = quizService.getResults(quiz.getId(), student.getId());
        assertThat(onlyStudent).hasSize(1);
        assertThat(onlyStudent.get(0).studentId()).isEqualTo(student.getId());
        assertThat(onlyStudent.get(0).passed()).isTrue();
    }

    @Test
    void getQuizResponseDoesNotExposeCorrectness() {
        Quiz quiz = factory.quizWithMixedQuestions(module);
        QuizResponse response = quizService.findById(quiz.getId());

        // в ответе только id и текст варианта, isCorrect наружу не отдается
        assertThat(response.questions()).hasSize(2);
        assertThat(response.questions().get(0).options()).isNotEmpty();
        assertThat(com.example.eduplatform.dto.OptionResponse.class.getRecordComponents())
                .extracting(java.lang.reflect.RecordComponent::getName)
                .containsExactly("id", "optionText");
    }

    @Test
    void unknownQuizThrowsNotFound() {
        assertThatThrownBy(() -> quizService.findById(999_999_999L))
                .isInstanceOf(NotFoundException.class);
    }

    private QuizSubmissionResponse takeAllCorrect(Quiz quiz) {
        Question single = factory.questionByText(quiz, "Вопрос с одним ответом");
        Question multiple = factory.questionByText(quiz, "Вопрос с несколькими ответами");
        return quizService.take(quiz.getId(), new TakeQuizRequest(student.getId(), Map.of(
                single.getId(), List.of(factory.optionByText(single, "Верный ответ А").getId()),
                multiple.getId(), List.of(factory.optionByText(multiple, "Множественный 1").getId(),
                        factory.optionByText(multiple, "Множественный 2").getId()))));
    }

    private QuizResponse createStandardQuiz() {
        return quizService.createForModule(module.getId(), new CreateQuizRequest(
                "Тест по модулю", 15,
                List.of(
                        new QuestionRequest("Сколько будет 2+2?", QuestionType.SINGLE_CHOICE, 1,
                                List.of(new OptionRequest("4", true), new OptionRequest("5", false))),
                        new QuestionRequest("Выберите ORM-фреймворки", QuestionType.MULTIPLE_CHOICE, 2,
                                List.of(new OptionRequest("Hibernate", true),
                                        new OptionRequest("EclipseLink", true),
                                        new OptionRequest("jQuery", false))))));
    }
}
