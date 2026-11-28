package com.example.eduplatform.web;

import com.example.eduplatform.dto.GradeSubmissionRequest;
import com.example.eduplatform.dto.SubmissionResponse;
import com.example.eduplatform.service.SubmissionService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// rest api решений: просмотр, оценивание преподавателем, удаление
@RestController
@RequestMapping("/api/submissions")
public class SubmissionController {

    private final SubmissionService submissionService;

    public SubmissionController(SubmissionService submissionService) {
        this.submissionService = submissionService;
    }

    @GetMapping("/{id}")
    public SubmissionResponse findById(@PathVariable Long id) {
        return submissionService.findById(id);
    }

    // преподаватель ставит оценку и комментарий
    @PutMapping("/{id}/grade")
    public SubmissionResponse grade(@PathVariable Long id, @Valid @RequestBody GradeSubmissionRequest request) {
        return submissionService.grade(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        submissionService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
