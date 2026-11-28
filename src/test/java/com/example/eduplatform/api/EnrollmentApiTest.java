package com.example.eduplatform.api;

import com.example.eduplatform.dto.EnrollRequest;
import com.example.eduplatform.dto.EnrollmentUpdateRequest;
import com.example.eduplatform.entity.Course;
import com.example.eduplatform.entity.User;
import com.example.eduplatform.support.TestDataFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// rest api записи на курс: enroll/unenroll, уникальность, статусы, списки
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class EnrollmentApiTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private TestDataFactory factory;

    @Test
    void enrollViaCourseEndpointReturns201() throws Exception {
        Course course = factory.course(factory.teacher(), factory.category());
        User student = factory.student();

        mockMvc.perform(post("/api/courses/{id}/enroll", course.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new EnrollRequest(student.getId()))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.studentId").value(student.getId()))
                .andExpect(jsonPath("$.courseTitle").value(course.getTitle()))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.enrolledAt").exists());
    }

    @Test
    void duplicateEnrollmentReturns409() throws Exception {
        Course course = factory.course(factory.teacher(), factory.category());
        User student = factory.student();

        mockMvc.perform(post("/api/courses/{id}/enroll", course.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new EnrollRequest(student.getId()))))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/courses/{id}/enroll", course.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new EnrollRequest(student.getId()))))
                .andExpect(status().isConflict());
    }

    @Test
    void enrollmentsAreListedByCourseAndByStudent() throws Exception {
        Course course = factory.course(factory.teacher(), factory.category());
        User student = factory.student();
        mockMvc.perform(post("/api/courses/{id}/enroll", course.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new EnrollRequest(student.getId()))))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/enrollments").param("courseId", String.valueOf(course.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].studentName").value(student.getName()));

        mockMvc.perform(get("/api/enrollments").param("studentId", String.valueOf(student.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].courseTitle").value(course.getTitle()));
    }

    @Test
    void statusCanBeChangedToCompleted() throws Exception {
        Course course = factory.course(factory.teacher(), factory.category());
        User student = factory.student();

        String body = mockMvc.perform(post("/api/courses/{id}/enroll", course.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new EnrollRequest(student.getId()))))
                .andReturn().getResponse().getContentAsString();
        long enrollmentId = ((Number) com.jayway.jsonpath.JsonPath.read(body, "$.id")).longValue();

        mockMvc.perform(put("/api/enrollments/{id}", enrollmentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new EnrollmentUpdateRequest(
                                com.example.eduplatform.entity.enums.EnrollmentStatus.COMPLETED))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    @Test
    void unenrollReturns204ThenGetReturns404() throws Exception {
        Course course = factory.course(factory.teacher(), factory.category());
        User student = factory.student();

        String body = mockMvc.perform(post("/api/courses/{id}/enroll", course.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new EnrollRequest(student.getId()))))
                .andReturn().getResponse().getContentAsString();
        long enrollmentId = ((Number) com.jayway.jsonpath.JsonPath.read(body, "$.id")).longValue();

        mockMvc.perform(delete("/api/enrollments/{id}", enrollmentId))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/enrollments/{id}", enrollmentId))
                .andExpect(status().isNotFound());
    }
}
