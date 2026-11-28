package com.example.eduplatform.api;

import com.example.eduplatform.dto.CreateUserRequest;
import com.example.eduplatform.dto.ProfileRequest;
import com.example.eduplatform.dto.UpdateUserRequest;
import com.example.eduplatform.entity.User;
import com.example.eduplatform.entity.enums.UserRole;
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

// rest api юзеров: коды ответов, валидация, обработка ошибок
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class UserApiTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private TestDataFactory factory;

    @Test
    void createUserReturns201() throws Exception {
        var request = new CreateUserRequest("Анна API", "anna.api." + System.nanoTime() + "@test.io", UserRole.ADMIN);

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.name").value("Анна API"))
                .andExpect(jsonPath("$.role").value("ADMIN"))
                .andExpect(jsonPath("$.createdAt").exists());
    }

    @Test
    void createUserWithInvalidDataReturns400WithFieldErrors() throws Exception {
        var request = new CreateUserRequest("", "не-email", UserRole.STUDENT);

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.fieldErrors.name").exists())
                .andExpect(jsonPath("$.fieldErrors.email").exists());
    }

    @Test
    void duplicateEmailReturns409() throws Exception {
        User existing = factory.student();
        var request = new CreateUserRequest("Дубль", existing.getEmail(), UserRole.STUDENT);

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("уже существует")));
    }

    @Test
    void getUserReturns200AndUnknownReturns404() throws Exception {
        User user = factory.student();

        mockMvc.perform(get("/api/users/{id}", user.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(user.getEmail()));

        mockMvc.perform(get("/api/users/{id}", 999_999_999))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("не найден")));
    }

    @Test
    void updateUserChangesName() throws Exception {
        User user = factory.student();
        var request = new UpdateUserRequest("Обновлённое имя", null, null, null);

        mockMvc.perform(put("/api/users/{id}", user.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Обновлённое имя"))
                .andExpect(jsonPath("$.email").value(user.getEmail()));
    }

    @Test
    void deleteUserReturns204ThenGetReturns404() throws Exception {
        User user = factory.student();

        mockMvc.perform(delete("/api/users/{id}", user.getId()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/users/{id}", user.getId()))
                .andExpect(status().isNotFound());
    }

    @Test
    void profileIsCreatedAndReadViaApi() throws Exception {
        User user = factory.student();
        var profile = new ProfileRequest("Биография API", "http://avatar", "+7 999", "Москва", null);

        mockMvc.perform(put("/api/users/{id}/profile", user.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(profile)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bio").value("Биография API"))
                .andExpect(jsonPath("$.userId").value(user.getId()));

        mockMvc.perform(get("/api/users/{id}/profile", user.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.city").value("Москва"));
    }

    @Test
    void listAllUsersIsReturned() throws Exception {
        User user = factory.student();

        mockMvc.perform(get("/api/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[?(@.id == " + user.getId() + ")]").exists());
    }
}
