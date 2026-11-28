package com.example.eduplatform.integration;

import com.example.eduplatform.dto.SubmissionResponse;
import com.example.eduplatform.entity.Assignment;
import com.example.eduplatform.entity.Course;
import com.example.eduplatform.entity.Lesson;
import com.example.eduplatform.entity.Module;
import com.example.eduplatform.entity.User;
import com.example.eduplatform.entity.enums.NotificationType;
import com.example.eduplatform.entity.enums.SubmissionStatus;
import com.example.eduplatform.exception.BusinessRuleException;
import com.example.eduplatform.exception.DuplicateResourceException;
import com.example.eduplatform.exception.NotFoundException;
import com.example.eduplatform.repository.NotificationRepository;
import com.example.eduplatform.repository.SubmissionRepository;
import com.example.eduplatform.service.SubmissionService;
import com.example.eduplatform.support.TestDataFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// полный цикл домашки: отправка решения, проверка уникальности, оценка преподавателем, уведомления
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class SubmissionServiceIntegrationTest {

    @Autowired private SubmissionService submissionService;
    @Autowired private TestDataFactory factory;
    @Autowired private SubmissionRepository submissionRepository;
    @Autowired private NotificationRepository notificationRepository;

    private User teacher;
    private User student;
    private Assignment assignment;
    private Course course;

    @BeforeEach
    void setUp() {
        teacher = factory.teacher();
        student = factory.student();
        course = factory.course(teacher, factory.category());
        Module module = factory.module(course);
        Lesson lesson = factory.lesson(module);
        assignment = factory.assignment(lesson, LocalDateTime.now().plusDays(7));
        factory.enroll(student, course);
    }

    @Test
    void studentSubmitsSolution() {
        var response = submit();

        assertThat(response.id()).isNotNull();
        assertThat(response.assignmentId()).isEqualTo(assignment.getId());
        assertThat(response.studentId()).isEqualTo(student.getId());
        assertThat(response.status()).isEqualTo(SubmissionStatus.SUBMITTED);
        assertThat(response.submittedAt()).isNotNull();
        assertThat(response.score()).isNull();
        assertThat(submissionRepository.findById(response.id())).isPresent();
    }

    @Test
    void duplicateSubmissionIsRejected() {
        submit();

        assertThatThrownBy(this::submit)
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("уже отправлено");
    }

    @Test
    void lateSubmissionIsRejected() {
        Assignment overdue = factory.assignment(factory.lesson(factory.module(course)),
                LocalDateTime.now().minusDays(1));

        assertThatThrownBy(() -> submissionService.submit(overdue.getId(),
                new com.example.eduplatform.dto.SubmitSolutionRequest(student.getId(), "Решение", null)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Дедлайн");
    }

    @Test
    void notEnrolledStudentCannotSubmit() {
        User outsider = factory.student();

        assertThatThrownBy(() -> submissionService.submit(assignment.getId(),
                new com.example.eduplatform.dto.SubmitSolutionRequest(outsider.getId(), "Решение", null)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("не записан");
    }

    @Test
    void nonStudentCannotSubmit() {
        assertThatThrownBy(() -> submissionService.submit(assignment.getId(),
                new com.example.eduplatform.dto.SubmitSolutionRequest(teacher.getId(), "Решение", null)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("студент");
    }

    @Test
    void teacherGradesSubmissionAndStudentIsNotified() {
        var submitted = submit();

        var graded = submissionService.grade(submitted.id(),
                new com.example.eduplatform.dto.GradeSubmissionRequest(85, "Хорошая работа"));

        assertThat(graded.score()).isEqualTo(85);
        assertThat(graded.feedback()).isEqualTo("Хорошая работа");
        assertThat(graded.status()).isEqualTo(SubmissionStatus.GRADED);

        // оценка реально сохранилась в бд
        var fromDb = submissionRepository.findById(submitted.id()).orElseThrow();
        assertThat(fromDb.getScore()).isEqualTo(85);
        assertThat(fromDb.getStatus()).isEqualTo(SubmissionStatus.GRADED);

        // студенту пришло уведомление
        var notifications = notificationRepository.findByRecipientIdOrderByCreatedAtDesc(student.getId());
        assertThat(notifications).anyMatch(n -> n.getType() == NotificationType.SUBMISSION_GRADED);
    }

    @Test
    void gradeAboveMaxScoreIsRejected() {
        var submitted = submit();

        assertThatThrownBy(() -> submissionService.grade(submitted.id(),
                new com.example.eduplatform.dto.GradeSubmissionRequest(assignment.getMaxScore() + 1, null)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("превышает");
    }

    @Test
    void gradeUnknownSubmissionThrowsNotFound() {
        assertThatThrownBy(() -> submissionService.grade(999_999_999L,
                new com.example.eduplatform.dto.GradeSubmissionRequest(10, null)))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void listsByAssignmentAndByStudent() {
        User student2 = factory.student();
        factory.enroll(student2, course);
        var first = submit();
        var second = submissionService.submit(assignment.getId(),
                new com.example.eduplatform.dto.SubmitSolutionRequest(student2.getId(), "Решение второго", null));

        var byAssignment = submissionService.findByAssignment(assignment.getId());
        assertThat(byAssignment).extracting(SubmissionResponse::id)
                .containsExactlyInAnyOrder(first.id(), second.id());

        var byStudent = submissionService.findByStudent(student.getId());
        assertThat(byStudent).extracting(SubmissionResponse::id).containsExactly(first.id());
        assertThat(byStudent.get(0).assignmentTitle()).isEqualTo(assignment.getTitle());
    }

    @Test
    void deletedSubmissionCanBeResubmitted() {
        var submitted = submit();
        submissionService.delete(submitted.id());
        assertThat(submissionRepository.findById(submitted.id())).isEmpty();

        // после удаления можно сдать заново
        var resubmitted = submit();
        assertThat(resubmitted.id()).isNotEqualTo(submitted.id());
        assertThat(resubmitted.status()).isEqualTo(SubmissionStatus.SUBMITTED);
    }

    private SubmissionResponse submit() {
        return submissionService.submit(assignment.getId(),
                new com.example.eduplatform.dto.SubmitSolutionRequest(student.getId(),
                        "Моё решение: проект с сущностями и репозиториями.", null));
    }
}
