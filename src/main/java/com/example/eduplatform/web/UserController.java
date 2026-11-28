package com.example.eduplatform.web;

import com.example.eduplatform.dto.CreateUserRequest;
import com.example.eduplatform.dto.EnrollmentResponse;
import com.example.eduplatform.dto.NotificationResponse;
import com.example.eduplatform.dto.ProfileRequest;
import com.example.eduplatform.dto.ProfileResponse;
import com.example.eduplatform.dto.QuizSubmissionResponse;
import com.example.eduplatform.dto.SubmissionResponse;
import com.example.eduplatform.dto.UpdateUserRequest;
import com.example.eduplatform.dto.UserDetailResponse;
import com.example.eduplatform.dto.UserResponse;
import com.example.eduplatform.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// rest api юзеров /api/users
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping
    public ResponseEntity<UserResponse> create(@Valid @RequestBody CreateUserRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.create(request));
    }

    @GetMapping
    public List<UserResponse> findAll() {
        return userService.findAll();
    }

    @GetMapping("/{id}")
    public UserResponse findById(@PathVariable Long id) {
        return userService.findById(id);
    }

    @GetMapping("/{id}/detail")
    public UserDetailResponse findDetailById(@PathVariable Long id) {
        return userService.findDetailById(id);
    }

    @PutMapping("/{id}")
    public UserResponse update(@PathVariable Long id, @Valid @RequestBody UpdateUserRequest request) {
        return userService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        userService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/profile")
    public ProfileResponse getProfile(@PathVariable Long id) {
        return userService.getProfile(id);
    }

    @PutMapping("/{id}/profile")
    public ProfileResponse updateProfile(@PathVariable Long id, @Valid @RequestBody ProfileRequest request) {
        return userService.updateProfile(id, request);
    }

    @GetMapping("/{id}/courses")
    public List<EnrollmentResponse> getUserCourses(@PathVariable Long id) {
        return userService.getCourses(id);
    }

    @GetMapping("/{id}/submissions")
    public List<SubmissionResponse> getUserSubmissions(@PathVariable Long id) {
        return userService.getSubmissions(id);
    }

    @GetMapping("/{id}/quiz-results")
    public List<QuizSubmissionResponse> getUserQuizResults(@PathVariable Long id) {
        return userService.getQuizResults(id);
    }

    @GetMapping("/{id}/notifications")
    public List<NotificationResponse> getNotifications(@PathVariable Long id) {
        return userService.getNotifications(id);
    }
}
