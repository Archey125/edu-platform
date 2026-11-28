package com.example.eduplatform.integration;

import com.example.eduplatform.dto.EnrollmentResponse;
import com.example.eduplatform.entity.Course;
import com.example.eduplatform.entity.User;
import com.example.eduplatform.exception.BusinessRuleException;
import com.example.eduplatform.exception.DuplicateResourceException;
import com.example.eduplatform.exception.NotFoundException;
import com.example.eduplatform.entity.enums.EnrollmentStatus;
import com.example.eduplatform.entity.enums.NotificationType;
import com.example.eduplatform.repository.EnrollmentRepository;
import com.example.eduplatform.repository.NotificationRepository;
import com.example.eduplatform.service.EnrollmentService;
import com.example.eduplatform.support.TestDataFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// запись на курс: уникальность, роли, отписка, списки, уведомления
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class EnrollmentServiceIntegrationTest {

    @Autowired private EnrollmentService enrollmentService;
    @Autowired private TestDataFactory factory;
    @Autowired private EnrollmentRepository enrollmentRepository;
    @Autowired private NotificationRepository notificationRepository;

    @Test
    void enrollStudentSuccessfully() {
        User teacher = factory.teacher();
        Course course = factory.course(teacher, factory.category());
        User student = factory.student();

        EnrollmentResponse enrollment = enrollmentService.enroll(course.getId(), student.getId());

        assertThat(enrollment.id()).isNotNull();
        assertThat(enrollment.studentId()).isEqualTo(student.getId());
        assertThat(enrollment.courseId()).isEqualTo(course.getId());
        assertThat(enrollment.status()).isEqualTo(EnrollmentStatus.ACTIVE);
        assertThat(enrollment.enrolledAt()).isNotNull();
        assertThat(enrollmentRepository.findById(enrollment.id())).isPresent();
    }

    @Test
    void duplicateEnrollmentIsRejected() {
        Course course = factory.course(factory.teacher(), factory.category());
        User student = factory.student();
        enrollmentService.enroll(course.getId(), student.getId());

        assertThatThrownBy(() -> enrollmentService.enroll(course.getId(), student.getId()))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("уже записан");
    }

    @Test
    void nonStudentCannotEnroll() {
        Course course = factory.course(factory.teacher(), factory.category());
        User teacher = factory.teacher();

        assertThatThrownBy(() -> enrollmentService.enroll(course.getId(), teacher.getId()))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("студент");
    }

    @Test
    void enrollWithUnknownUserOrCourseThrowsNotFound() {
        Course course = factory.course(factory.teacher(), factory.category());
        assertThatThrownBy(() -> enrollmentService.enroll(course.getId(), 999_999_999L))
                .isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> enrollmentService.enroll(999_999_999L, factory.student().getId()))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void unenrollRemovesEnrollment() {
        Course course = factory.course(factory.teacher(), factory.category());
        User student = factory.student();
        EnrollmentResponse enrollment = enrollmentService.enroll(course.getId(), student.getId());

        enrollmentService.unenroll(enrollment.id());

        assertThat(enrollmentRepository.findById(enrollment.id())).isEmpty();
        assertThatThrownBy(() -> enrollmentService.unenroll(enrollment.id()))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void statusCanBeUpdatedToCompleted() {
        Course course = factory.course(factory.teacher(), factory.category());
        User student = factory.student();
        EnrollmentResponse enrollment = enrollmentService.enroll(course.getId(), student.getId());

        var updated = enrollmentService.updateStatus(enrollment.id(),
                new com.example.eduplatform.dto.EnrollmentUpdateRequest(EnrollmentStatus.COMPLETED));

        assertThat(updated.status()).isEqualTo(EnrollmentStatus.COMPLETED);
    }

    @Test
    void listsWorkInBothDirections() {
        User teacher = factory.teacher();
        Course course1 = factory.course(teacher, factory.category());
        Course course2 = factory.course(teacher, factory.category());
        User student = factory.student();
        User student2 = factory.student();

        enrollmentService.enroll(course1.getId(), student.getId());
        enrollmentService.enroll(course2.getId(), student.getId());
        enrollmentService.enroll(course1.getId(), student2.getId());

        assertThat(enrollmentService.findByCourse(course1.getId())).hasSize(2);
        assertThat(enrollmentService.findByStudent(student.getId())).hasSize(2);
        assertThat(enrollmentService.findByStudent(student2.getId())).hasSize(1);
        assertThat(enrollmentService.findByCourse(course2.getId()))
                .allMatch(e -> e.courseTitle().equals(course2.getTitle()));
    }

    @Test
    void teacherIsNotifiedAboutNewEnrollment() {
        User teacher = factory.teacher();
        Course course = factory.course(teacher, factory.category());
        User student = factory.student();

        enrollmentService.enroll(course.getId(), student.getId());

        var notifications = notificationRepository.findByRecipientIdOrderByCreatedAtDesc(teacher.getId());
        assertThat(notifications).isNotEmpty();
        assertThat(notifications.get(0).getType()).isEqualTo(NotificationType.ENROLLMENT);
        assertThat(notifications.get(0).getMessage()).contains(student.getName());
    }
}
