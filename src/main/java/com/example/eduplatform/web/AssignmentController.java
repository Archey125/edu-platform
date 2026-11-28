package com.example.eduplatform.web;

import com.example.eduplatform.dto.AssignmentResponse;
import com.example.eduplatform.dto.SubmissionResponse;
import com.example.eduplatform.dto.SubmitSolutionRequest;
import com.example.eduplatform.service.AssignmentService;
import com.example.eduplatform.service.SubmissionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// rest api заданий и отправки решений /api/assignments
@RestController
@RequestMapping("/api/assignments")
public class AssignmentController {

    private final AssignmentService assignmentService;
    private final SubmissionService submissionService;

    public AssignmentController(AssignmentService assignmentService, SubmissionService submissionService) {
        this.assignmentService = assignmentService;
        this.submissionService = submissionService;
    }

    @GetMapping("/{id}")
    public AssignmentResponse findById(@PathVariable Long id) {
        return assignmentService.findById(id);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        assignmentService.delete(id);
        return ResponseEntity.noContent().build();
    }

    // решения по заданию, для проверки преподавателем
    @GetMapping("/{id}/submissions")
    public List<SubmissionResponse> getSubmissions(@PathVariable Long id) {
        return assignmentService.getSubmissions(id);
    }

    // студент отправляет решение
    @PostMapping("/{id}/submit")
    public ResponseEntity<SubmissionResponse> submit(@PathVariable Long id,
                                                     @Valid @RequestBody SubmitSolutionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(submissionService.submit(id, request));
    }
}
