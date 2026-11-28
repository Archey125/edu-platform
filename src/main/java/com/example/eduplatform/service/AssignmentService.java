package com.example.eduplatform.service;

import com.example.eduplatform.dto.AssignmentResponse;
import com.example.eduplatform.dto.SubmissionResponse;
import com.example.eduplatform.entity.Assignment;
import com.example.eduplatform.exception.NotFoundException;
import com.example.eduplatform.repository.AssignmentRepository;
import com.example.eduplatform.repository.SubmissionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

// задания: чтение, удаление, выборка решений по заданию
@Service
@Transactional(readOnly = true)
public class AssignmentService {

    private static final Logger log = LoggerFactory.getLogger(AssignmentService.class);

    private final AssignmentRepository assignmentRepository;
    private final SubmissionRepository submissionRepository;

    public AssignmentService(AssignmentRepository assignmentRepository, SubmissionRepository submissionRepository) {
        this.assignmentRepository = assignmentRepository;
        this.submissionRepository = submissionRepository;
    }

    public AssignmentResponse findById(Long id) {
        return toResponse(getAssignment(id));
    }

    // удаление задания каскадно удалит все решения по нему
    @Transactional
    public void delete(Long id) {
        Assignment assignment = getAssignment(id);
        assignmentRepository.delete(assignment);
        log.info("Удалено задание '{}' (id={}) вместе с решениями", assignment.getTitle(), id);
    }

    // все решения по заданию
    public List<SubmissionResponse> getSubmissions(Long assignmentId) {
        getAssignment(assignmentId);
        return submissionRepository.findWithStudentByAssignmentId(assignmentId).stream()
                .map(SubmissionService::toResponse)
                .toList();
    }

    private Assignment getAssignment(Long id) {
        return assignmentRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Задание с id=" + id + " не найдено"));
    }

    public static AssignmentResponse toResponse(Assignment assignment) {
        return new AssignmentResponse(assignment.getId(), assignment.getLesson().getId(),
                assignment.getTitle(), assignment.getDescription(), assignment.getDueDate(),
                assignment.getMaxScore());
    }
}
