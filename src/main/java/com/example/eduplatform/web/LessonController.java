package com.example.eduplatform.web;

import com.example.eduplatform.dto.AssignmentRequest;
import com.example.eduplatform.dto.AssignmentResponse;
import com.example.eduplatform.dto.LessonRequest;
import com.example.eduplatform.dto.LessonResponse;
import com.example.eduplatform.service.LessonService;
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

// rest api уроков и заданий уроков /api/lessons
@RestController
@RequestMapping("/api/lessons")
public class LessonController {

    private final LessonService lessonService;

    public LessonController(LessonService lessonService) {
        this.lessonService = lessonService;
    }

    @GetMapping("/{id}")
    public LessonResponse findById(@PathVariable Long id) {
        return lessonService.findById(id);
    }

    @PutMapping("/{id}")
    public LessonResponse update(@PathVariable Long id, @Valid @RequestBody LessonRequest request) {
        return lessonService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        lessonService.delete(id);
        return ResponseEntity.noContent().build();
    }

    // добавить задание к уроку
    @PostMapping("/{id}/assignments")
    public ResponseEntity<AssignmentResponse> addAssignment(@PathVariable Long id,
                                                            @Valid @RequestBody AssignmentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(lessonService.addAssignment(id, request));
    }
}
