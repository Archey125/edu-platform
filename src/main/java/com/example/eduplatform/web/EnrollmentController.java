package com.example.eduplatform.web;

import com.example.eduplatform.dto.EnrollmentCreateRequest;
import com.example.eduplatform.dto.EnrollmentResponse;
import com.example.eduplatform.dto.EnrollmentUpdateRequest;
import com.example.eduplatform.service.EnrollmentService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// rest api записей на курс /api/enrollments
// get ?courseId=... - кто записан на курс, get ?studentId=... - курсы студента
@RestController
@RequestMapping("/api/enrollments")
public class EnrollmentController {

    private final EnrollmentService enrollmentService;

    public EnrollmentController(EnrollmentService enrollmentService) {
        this.enrollmentService = enrollmentService;
    }

    @PostMapping
    public ResponseEntity<EnrollmentResponse> enroll(@Valid @RequestBody EnrollmentCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(enrollmentService.enroll(request));
    }

    @GetMapping
    public List<EnrollmentResponse> findAll(
            @RequestParam(required = false) Long courseId,
            @RequestParam(required = false) Long studentId) {
        if (courseId != null) {
            return enrollmentService.findByCourse(courseId);
        }
        if (studentId != null) {
            return enrollmentService.findByStudent(studentId);
        }
        return List.of();
    }

    @GetMapping("/{id}")
    public EnrollmentResponse findById(@PathVariable Long id) {
        return enrollmentService.findById(id);
    }

    @PutMapping("/{id}")
    public EnrollmentResponse updateStatus(@PathVariable Long id,
                                           @Valid @RequestBody EnrollmentUpdateRequest request) {
        return enrollmentService.updateStatus(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> unenroll(@PathVariable Long id) {
        enrollmentService.unenroll(id);
        return ResponseEntity.noContent().build();
    }
}
