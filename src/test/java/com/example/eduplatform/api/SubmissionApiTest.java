package com.example.eduplatform.api;

import com.example.eduplatform.dto.GradeSubmissionRequest;
import com.example.eduplatform.dto.SubmitSolutionRequest;
import com.example.eduplatform.entity.Assignment;
import com.example.eduplatform.entity.Course;
import com.example.eduplatform.entity.Lesson;
import com.example.eduplatform.entity.Module;
import com.example.eduplatform.entity.User;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// rest api решений: отправка, уникальность, оценивание, ошибки валидации
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class SubmissionApiTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private TestDataFactory factory;

    private User student;
    private Assignment assignment;
    private Course course;

    @BeforeEach
    void setUp() {
        User teacher = factory.teacher();
        student = factory.student();
        course = factory.course(teacher, factory.category());
        Module module = factory.module(course);
        Lesson lesson = factory.lesson(module);
        assignment = factory.assignment(lesson);
        factory.enroll(student, course);
    }

    @Test
    void submitReturns201() throws Exception {
        mockMvc.perform(post("/api/assignments/{id}/submit", assignment.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new SubmitSolutionRequest(student.getId(), "Решение через API", null))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.assignmentTitle").value(assignment.getTitle()))
                .andExpect(jsonPath("$.studentName").value(student.getName()))
                .andExpect(jsonPath("$.status").value("SUBMITTED"))
                .andExpect(jsonPath("$.score").doesNotExist());
    }

    @Test
    void duplicateSubmitReturns409() throws Exception {
        mockMvc.perform(post("/api/assignments/{id}/submit", assignment.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new SubmitSolutionRequest(student.getId(), "Первое решение", null))))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/assignments/{id}/submit", assignment.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new SubmitSolutionRequest(student.getId(), "Второе решение", null))))
                .andExpect(status().isConflict());
    }

    @Test
    void blankContentReturns400WithFieldError() throws Exception {
        mockMvc.perform(post("/api/assignments/{id}/submit", assignment.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new SubmitSolutionRequest(student.getId(), "", null))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.content").exists());
    }

    @Test
    void notEnrolledStudentGets400() throws Exception {
        User outsider = factory.student();

        mockMvc.perform(post("/api/assignments/{id}/submit", assignment.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new SubmitSolutionRequest(outsider.getId(), "Решение", null))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("не записан")));
    }

    @Test
    void teacherGradesViaApiAndListShowsScore() throws Exception {
        String body = mockMvc.perform(post("/api/assignments/{id}/submit", assignment.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new SubmitSolutionRequest(student.getId(), "Решение на оценку", null))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long submissionId = ((Number) com.jayway.jsonpath.JsonPath.read(body, "$.id")).longValue();

        mockMvc.perform(put("/api/submissions/{id}/grade", submissionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new GradeSubmissionRequest(95, "Молодец!"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.score").value(95))
                .andExpect(jsonPath("$.feedback").value("Молодец!"))
                .andExpect(jsonPath("$.status").value("GRADED"));

        mockMvc.perform(get("/api/assignments/{id}/submissions", assignment.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].score").value(95))
                .andExpect(jsonPath("$[0].studentName").value(student.getName()));
    }

    @Test
    void gradeAboveMaxReturns400() throws Exception {
        String body = mockMvc.perform(post("/api/assignments/{id}/submit", assignment.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new SubmitSolutionRequest(student.getId(), "Решение", null))))
                .andReturn().getResponse().getContentAsString();
        long submissionId = ((Number) com.jayway.jsonpath.JsonPath.read(body, "$.id")).longValue();

        mockMvc.perform(put("/api/submissions/{id}/grade", submissionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new GradeSubmissionRequest(150, null))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("превышает")));
    }
}
