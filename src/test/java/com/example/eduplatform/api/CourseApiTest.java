package com.example.eduplatform.api;

import com.example.eduplatform.dto.CreateCourseRequest;
import com.example.eduplatform.dto.ModuleRequest;
import com.example.eduplatform.dto.UpdateCourseRequest;
import com.example.eduplatform.entity.Category;
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

// rest api курсов: создание, структура, модули, теги, фильтры, правила удаления
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class CourseApiTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private TestDataFactory factory;

    @Test
    void createCourseReturns201WithCategoryAndTeacher() throws Exception {
        User teacher = factory.teacher();
        Category category = factory.category();
        var request = new CreateCourseRequest("REST курс", "Описание", category.getId(),
                teacher.getId(), 30, null);

        mockMvc.perform(post("/api/courses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.categoryName").value(category.getName()))
                .andExpect(jsonPath("$.teacherName").value(teacher.getName()))
                .andExpect(jsonPath("$.modules.length()").value(0));
    }

    @Test
    void createCourseWithStudentAsTeacherReturns400() throws Exception {
        Category category = factory.category();
        User student = factory.student();
        var request = new CreateCourseRequest("Курс", null, category.getId(), student.getId(), null, null);

        mockMvc.perform(post("/api/courses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("преподавателем")));
    }

    @Test
    void courseDetailContainsModulesLessonsAssignments() throws Exception {
        User teacher = factory.teacher();
        Course course = factory.course(teacher, factory.category());
        var module = factory.module(course);
        var lesson = factory.lesson(module);
        var assignment = factory.assignment(lesson);

        mockMvc.perform(get("/api/courses/{id}", course.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.modules.length()").value(1))
                .andExpect(jsonPath("$.modules[0].lessons.length()").value(1))
                .andExpect(jsonPath("$.modules[0].lessons[0].assignments[0].id").value(assignment.getId()))
                .andExpect(jsonPath("$.modules[0].lessons[0].assignments[0].maxScore").value(100));
    }

    @Test
    void unknownCourseReturns404() throws Exception {
        mockMvc.perform(get("/api/courses/{id}", 999_999_999))
                .andExpect(status().isNotFound());
    }

    @Test
    void moduleIsAddedViaApi() throws Exception {
        Course course = factory.course(factory.teacher(), factory.category());
        var request = new ModuleRequest("Модуль через API", "Описание", null);

        mockMvc.perform(post("/api/courses/{id}/modules", course.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Модуль через API"))
                .andExpect(jsonPath("$.orderIndex").value(1));

        mockMvc.perform(get("/api/courses/{id}", course.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.modules.length()").value(1));
    }

    @Test
    void updateCourseReturns200() throws Exception {
        Course course = factory.course(factory.teacher(), factory.category());
        var request = new UpdateCourseRequest("Обновлённый курс", null, null, null, null, null);

        mockMvc.perform(put("/api/courses/{id}", course.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Обновлённый курс"));
    }

    @Test
    void deleteCourseWithoutEnrollmentsReturns204() throws Exception {
        Course course = factory.course(factory.teacher(), factory.category());

        mockMvc.perform(delete("/api/courses/{id}", course.getId()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/courses/{id}", course.getId()))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteCourseWithEnrollmentsReturns409() throws Exception {
        Course course = factory.course(factory.teacher(), factory.category());
        factory.enroll(factory.student(), course);

        mockMvc.perform(delete("/api/courses/{id}", course.getId()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("записи студентов")));
    }

    @Test
    void coursesCanBeFilteredByCategory() throws Exception {
        Category category = factory.category();
        Course course = factory.course(factory.teacher(), category);
        factory.course(factory.teacher(), factory.category());

        mockMvc.perform(get("/api/courses").param("categoryId", String.valueOf(category.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(course.getId()))
                .andExpect(jsonPath("$[0].categoryName").value(category.getName()));
    }

    @Test
    void tagsAreAttachedAndDetachedViaApi() throws Exception {
        Course course = factory.course(factory.teacher(), factory.category());
        var tag = factory.tag();

        mockMvc.perform(post("/api/courses/{id}/tags/{tagId}", course.getId(), tag.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tags[0]").value(tag.getName()));

        mockMvc.perform(delete("/api/courses/{id}/tags/{tagId}", course.getId(), tag.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tags.length()").value(0));
    }
}
