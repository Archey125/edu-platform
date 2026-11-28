package com.example.eduplatform.service;

import com.example.eduplatform.dto.GradeSubmissionRequest;
import com.example.eduplatform.dto.SubmissionResponse;
import com.example.eduplatform.dto.SubmitSolutionRequest;
import com.example.eduplatform.entity.Assignment;
import com.example.eduplatform.entity.Course;
import com.example.eduplatform.entity.Enrollment;
import com.example.eduplatform.entity.Submission;
import com.example.eduplatform.entity.User;
import com.example.eduplatform.entity.enums.EnrollmentStatus;
import com.example.eduplatform.entity.enums.NotificationType;
import com.example.eduplatform.entity.enums.SubmissionStatus;
import com.example.eduplatform.entity.enums.UserRole;
import com.example.eduplatform.exception.BusinessRuleException;
import com.example.eduplatform.exception.DuplicateResourceException;
import com.example.eduplatform.exception.NotFoundException;
import com.example.eduplatform.repository.AssignmentRepository;
import com.example.eduplatform.repository.EnrollmentRepository;
import com.example.eduplatform.repository.SubmissionRepository;
import com.example.eduplatform.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

// цикл домашки: студент сдает решение (одно на задание), преподаватель оценивает, все получают уведомления
@Service
@Transactional(readOnly = true)
public class SubmissionService {

    private static final Logger log = LoggerFactory.getLogger(SubmissionService.class);

    private final SubmissionRepository submissionRepository;
    private final AssignmentRepository assignmentRepository;
    private final UserRepository userRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final NotificationService notificationService;

    public SubmissionService(SubmissionRepository submissionRepository,
                             AssignmentRepository assignmentRepository,
                             UserRepository userRepository,
                             EnrollmentRepository enrollmentRepository,
                             NotificationService notificationService) {
        this.submissionRepository = submissionRepository;
        this.assignmentRepository = assignmentRepository;
        this.userRepository = userRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.notificationService = notificationService;
    }

    @Transactional
    public SubmissionResponse submit(Long assignmentId, SubmitSolutionRequest request) {
        Assignment assignment = assignmentRepository.findWithCourseContextById(assignmentId)
                .orElseThrow(() -> new NotFoundException("Задание с id=" + assignmentId + " не найдено"));
        User student = userRepository.findById(request.studentId())
                .orElseThrow(() -> new NotFoundException("Пользователь с id=" + request.studentId() + " не найден"));
        if (student.getRole() != UserRole.STUDENT) {
            throw new BusinessRuleException("Сдавать задания может только студент (пользователь '"
                    + student.getName() + "' имеет роль " + student.getRole() + ")");
        }
        Course course = assignment.getLesson().getModule().getCourse();
        Enrollment enrollment = enrollmentRepository
                .findByStudentIdAndCourseId(student.getId(), course.getId())
                .orElseThrow(() -> new BusinessRuleException("Студент '" + student.getName()
                        + "' не записан на курс «" + course.getTitle() + "» — решение не принимается"));
        if (enrollment.getStatus() != EnrollmentStatus.ACTIVE) {
            throw new BusinessRuleException("Запись на курс имеет статус " + enrollment.getStatus()
                    + " — сдача заданий невозможна");
        }
        if (assignment.getDueDate() != null && LocalDateTime.now().isAfter(assignment.getDueDate())) {
            throw new BusinessRuleException("Дедлайн задания «" + assignment.getTitle() + "» истёк ("
                    + assignment.getDueDate() + ") — решение не принимается");
        }
        if (submissionRepository.existsByAssignmentIdAndStudentId(assignmentId, student.getId())) {
            throw new DuplicateResourceException("Решение по заданию «" + assignment.getTitle()
                    + "» уже отправлено: у студента может быть только одно решение на задание");
        }
        Submission submission = new Submission();
        submission.setContent(request.content());
        submission.setFileUrl(request.fileUrl());
        submission.setStatus(SubmissionStatus.SUBMITTED);
        submission.setStudent(student);
        submission.setAssignment(assignment);
        Submission saved = submissionRepository.save(submission);
        notificationService.notify(course.getTeacher(), NotificationType.SUBMISSION_RECEIVED,
                "Студент " + student.getName() + " отправил решение по заданию «" + assignment.getTitle() + "»");
        log.info("Студент id={} отправил решение по заданию '{}' (id={})", student.getId(), assignment.getTitle(), assignmentId);
        return toResponse(saved);
    }

    @Transactional
    public SubmissionResponse grade(Long submissionId, GradeSubmissionRequest request) {
        Submission submission = submissionRepository.findWithStudentAndAssignmentById(submissionId)
                .orElseThrow(() -> new NotFoundException("Решение с id=" + submissionId + " не найдено"));
        Assignment assignment = submission.getAssignment();
        if (request.score() > assignment.getMaxScore()) {
            throw new BusinessRuleException("Оценка " + request.score()
                    + " превышает максимальный балл задания (" + assignment.getMaxScore() + ")");
        }
        submission.setScore(request.score());
        submission.setFeedback(request.feedback());
        submission.setStatus(SubmissionStatus.GRADED);
        notificationService.notify(submission.getStudent(), NotificationType.SUBMISSION_GRADED,
                "Задание «" + assignment.getTitle() + "» проверено: " + request.score()
                        + " из " + assignment.getMaxScore());
        log.info("Решение id={} оценено: {}/{}", submissionId, request.score(), assignment.getMaxScore());
        return toResponse(submission);
    }

    public SubmissionResponse findById(Long id) {
        return toResponse(submissionRepository.findWithStudentAndAssignmentById(id)
                .orElseThrow(() -> new NotFoundException("Решение с id=" + id + " не найдено")));
    }

    public List<SubmissionResponse> findByAssignment(Long assignmentId) {
        return submissionRepository.findWithStudentByAssignmentId(assignmentId).stream()
                .map(SubmissionService::toResponse)
                .toList();
    }

    public List<SubmissionResponse> findByStudent(Long studentId) {
        return submissionRepository.findWithDetailsByStudentId(studentId).stream()
                .map(SubmissionService::toResponse)
                .toList();
    }

    // студент может удалить свое решение и отправить новое
    @Transactional
    public void delete(Long id) {
        Submission submission = submissionRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Решение с id=" + id + " не найдено"));
        Long assignmentId = submission.getAssignment().getId();
        Long studentId = submission.getStudent().getId();
        submissionRepository.delete(submission);
        log.info("Удалено решение id={} (задание id={}, студент id={}) — теперь можно отправить новое",
                id, assignmentId, studentId);
    }

    public static SubmissionResponse toResponse(Submission submission) {
        return new SubmissionResponse(
                submission.getId(),
                submission.getAssignment().getId(),
                submission.getAssignment().getTitle(),
                submission.getStudent().getId(),
                submission.getStudent().getName(),
                submission.getContent(),
                submission.getFileUrl(),
                submission.getSubmittedAt(),
                submission.getScore(),
                submission.getFeedback(),
                submission.getStatus());
    }
}
