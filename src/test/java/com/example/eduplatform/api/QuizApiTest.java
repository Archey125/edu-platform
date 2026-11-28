package com.example.eduplatform.api;

import com.example.eduplatform.dto.CreateQuizRequest;
import com.example.eduplatform.dto.OptionRequest;
import com.example.eduplatform.dto.QuestionRequest;
import com.example.eduplatform.dto.TakeQuizRequest;
import com.example.eduplatform.entity.Course;
import com.example.eduplatform.entity.Module;
import com.example.eduplatform.entity.Question;
import com.example.eduplatform.entity.Quiz;
import com.example.eduplatform.entity.User;
import com.example.eduplatform.entity.enums.QuestionType;
import com.example.eduplatform.support.TestDataFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// rest api тестов: создание, скрытие правильных ответов, прохождение, запрет пересдачи
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class QuizApiTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private TestDataFactory factory;

    private User student;
    private Module module;
    private Course course;

    @BeforeEach
    void setUp() {
        User teacher = factory.teacher();
        student = factory.student();
        course = factory.course(teacher, factory.category());
        module = factory.module(course);
        factory.enroll(student, course);
    }

    @Test
    void createQuizViaApiReturns201() throws Exception {
        var request = new CreateQuizRequest("API тест", 10, List.of(
                new QuestionRequest("Вопрос 1", QuestionType.SINGLE_CHOICE, 1,
                        List.of(new OptionRequest("Да", true), new OptionRequest("Нет", false)))));

        mockMvc.perform(post("/api/modules/{id}/quiz", module.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.moduleId").value(module.getId()))
                .andExpect(jsonPath("$.questions.length()").value(1));
    }

    @Test
    void secondQuizForSameModuleReturns409() throws Exception {
        factory.quizWithMixedQuestions(module);
        var request = new CreateQuizRequest("Второй тест", null, List.of(
                new QuestionRequest("Вопрос", QuestionType.SINGLE_CHOICE, 1,
                        List.of(new OptionRequest("А", true), new OptionRequest("Б", false)))));

        mockMvc.perform(post("/api/modules/{id}/quiz", module.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    void quizOptionsDoNotExposeCorrectAnswers() throws Exception {
        Quiz quiz = factory.quizWithMixedQuestions(module);

        mockMvc.perform(get("/api/quizzes/{id}", quiz.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.questions.length()").value(2))
                .andExpect(jsonPath("$.questions[0].options[0].optionText").exists())
                .andExpect(jsonPath("$.questions[0].options[0].isCorrect").doesNotExist())
                .andExpect(jsonPath("$.questions[0].options[1].isCorrect").doesNotExist());
    }

    @Test
    void studentTakesQuizViaApi() throws Exception {
        Quiz quiz = factory.quizWithMixedQuestions(module);
        Question single = factory.questionByText(quiz, "Вопрос с одним ответом");
        Question multiple = factory.questionByText(quiz, "Вопрос с несколькими ответами");
        var request = new TakeQuizRequest(student.getId(), Map.of(
                single.getId(), List.of(factory.optionByText(single, "Верный ответ А").getId()),
                multiple.getId(), List.of(factory.optionByText(multiple, "Множественный 1").getId(),
                        factory.optionByText(multiple, "Множественный 2").getId())));

        mockMvc.perform(post("/api/quizzes/{id}/take", quiz.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.score").value(2))
                .andExpect(jsonPath("$.maxScore").value(2))
                .andExpect(jsonPath("$.percent").value(100))
                .andExpect(jsonPath("$.passed").value(true))
                .andExpect(jsonPath("$.takenAt").exists());
    }

    @Test
    void retakeViaApiReturns409() throws Exception {
        Quiz quiz = factory.quizWithMixedQuestions(module);
        Question single = factory.questionByText(quiz, "Вопрос с одним ответом");
        Question multiple = factory.questionByText(quiz, "Вопрос с несколькими ответами");
        var request = new TakeQuizRequest(student.getId(), Map.of(
                single.getId(), List.of(factory.optionByText(single, "Верный ответ А").getId()),
                multiple.getId(), List.of(factory.optionByText(multiple, "Множественный 1").getId(),
                        factory.optionByText(multiple, "Множественный 2").getId())));

        mockMvc.perform(post("/api/quizzes/{id}/take", quiz.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/quizzes/{id}/take", quiz.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("уже пройден")));
    }

    @Test
    void incompleteAnswersReturn400() throws Exception {
        Quiz quiz = factory.quizWithMixedQuestions(module);
        Question single = factory.questionByText(quiz, "Вопрос с одним ответом");
        // отвечен только один вопрос из двух
        var request = new TakeQuizRequest(student.getId(), Map.of(
                single.getId(), List.of(factory.optionByText(single, "Верный ответ А").getId())));

        mockMvc.perform(post("/api/quizzes/{id}/take", quiz.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("без ответа")));
    }

    @Test
    void resultsEndpointListsAttempts() throws Exception {
        Quiz quiz = factory.quizWithMixedQuestions(module);
        Question single = factory.questionByText(quiz, "Вопрос с одним ответом");
        Question multiple = factory.questionByText(quiz, "Вопрос с несколькими ответами");
        mockMvc.perform(post("/api/quizzes/{id}/take", quiz.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new TakeQuizRequest(student.getId(), Map.of(
                                single.getId(), List.of(factory.optionByText(single, "Верный ответ А").getId()),
                                multiple.getId(), List.of(factory.optionByText(multiple, "Множественный 1").getId(),
                                        factory.optionByText(multiple, "Множественный 2").getId()))))))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/quizzes/{id}/results", quiz.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].studentId").value(student.getId()))
                .andExpect(jsonPath("$[0].score").value(2));
    }
}
