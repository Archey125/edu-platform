package com.example.eduplatform.integration;

import com.example.eduplatform.entity.Course;
import com.example.eduplatform.entity.Enrollment;
import com.example.eduplatform.entity.Notification;
import com.example.eduplatform.entity.Profile;
import com.example.eduplatform.entity.Quiz;
import com.example.eduplatform.entity.QuizSubmission;
import com.example.eduplatform.entity.Submission;
import com.example.eduplatform.entity.User;
import com.example.eduplatform.exception.ConflictException;
import com.example.eduplatform.repository.CourseReviewRepository;
import com.example.eduplatform.repository.EnrollmentRepository;
import com.example.eduplatform.repository.NotificationRepository;
import com.example.eduplatform.repository.ProfileRepository;
import com.example.eduplatform.repository.QuizSubmissionRepository;
import com.example.eduplatform.repository.SubmissionRepository;
import com.example.eduplatform.repository.UserRepository;
import com.example.eduplatform.service.UserService;
import com.example.eduplatform.support.TestDataFactory;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// каскадное удаление юзера и правила 1-1 с профилем
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class UserCascadeTest {

    @Autowired private UserService userService;
    @Autowired private TestDataFactory factory;
    @Autowired private ProfileRepository profileRepository;
    @Autowired private EnrollmentRepository enrollmentRepository;
    @Autowired private SubmissionRepository submissionRepository;
    @Autowired private QuizSubmissionRepository quizSubmissionRepository;
    @Autowired private CourseReviewRepository reviewRepository;
    @Autowired private NotificationRepository notificationRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private EntityManager entityManager;

    // сброс persistence context: bulk удаления в сервисе минуют кеш первого уровня,
    // поэтому чтобы проверить реальное состояние бд нужен flush+clear
    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }

    @Test
    void deleteUserCascadesAllRelatedEntities() {
        User student = factory.student();
        User teacher = factory.teacher();
        Course course = factory.course(teacher, factory.category());
        Quiz quiz = factory.quizWithMixedQuestions(factory.module(course));

        // активность студента: профиль, запись, решение, результат теста, отзыв, уведомление
        Profile profile = new Profile();
        profile.setUser(student);
        profile.setBio("Тестовый профиль");
        student.setProfile(profile);
        profileRepository.save(profile);

        Enrollment enrollment = factory.enroll(student, course);
        Submission submission = factory.submission(factory.assignment(factory.lesson(factory.module(course))), student);
        QuizSubmission quizSubmission = new QuizSubmission();
        quizSubmission.setQuiz(quiz);
        quizSubmission.setStudent(student);
        quizSubmission.setScore(1);
        quizSubmission.setMaxScore(2);
        quizSubmission.setPassed(false);
        quizSubmissionRepository.save(quizSubmission);
        com.example.eduplatform.entity.CourseReview review = new com.example.eduplatform.entity.CourseReview();
        review.setStudent(student);
        review.setCourse(course);
        review.setRating(5);
        reviewRepository.save(review);
        notificationRepository.save(new Notification(student,
                com.example.eduplatform.entity.enums.NotificationType.INFO, "Тестовое уведомление"));

        // все на месте
        assertThat(profileRepository.findByUserId(student.getId())).isPresent();
        assertThat(enrollmentRepository.findById(enrollment.getId())).isPresent();
        assertThat(submissionRepository.findById(submission.getId())).isPresent();
        assertThat(quizSubmissionRepository.findById(quizSubmission.getId())).isPresent();
        assertThat(reviewRepository.findById(review.getId())).isPresent();

        // удаляем юзера, сервис удаляет все его данные.
        // bulk запросы минуют кеш первого уровня, поэтому проверяем фактическую бд после flush+clear
        userService.delete(student.getId());
        flushAndClear();

        assertThat(profileRepository.findByUserId(student.getId())).isEmpty();
        assertThat(enrollmentRepository.findById(enrollment.getId())).isEmpty();
        assertThat(submissionRepository.findById(submission.getId())).isEmpty();
        assertThat(quizSubmissionRepository.findById(quizSubmission.getId())).isEmpty();
        assertThat(reviewRepository.findById(review.getId())).isEmpty();
        assertThat(userRepository.findById(student.getId())).isEmpty();
    }

    @Test
    void teacherWithCoursesCannotBeDeleted() {
        User teacher = factory.teacher();
        factory.course(teacher, factory.category());

        assertThatThrownBy(() -> userService.delete(teacher.getId()))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("преподавателя");
    }

    @Test
    void teacherWithoutCoursesCanBeDeleted() {
        User teacher = factory.teacher();
        userService.delete(teacher.getId());
        assertThatThrownBy(() -> userService.findById(teacher.getId()))
                .isInstanceOf(com.example.eduplatform.exception.NotFoundException.class);
    }

    @Test
    void profileIsUpsertedOneToOne() {
        User student = factory.student();

        var created = userService.updateProfile(student.getId(),
                new com.example.eduplatform.dto.ProfileRequest("Первая биография", null, null, "Москва", null));
        assertThat(created.id()).isNotNull();

        var updated = userService.updateProfile(student.getId(),
                new com.example.eduplatform.dto.ProfileRequest("Вторая биография", null, null, null, null));
        // профиль тот же (id не поменялся), данные обновились
        assertThat(updated.id()).isEqualTo(created.id());
        assertThat(updated.bio()).isEqualTo("Вторая биография");

        var fromDb = userService.getProfile(student.getId());
        assertThat(fromDb.bio()).isEqualTo("Вторая биография");
    }
}
