package com.example.eduplatform.integration;

import com.example.eduplatform.config.DataSeeder;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

// сидер запускается при старте контекста (app.data-seeder.enabled=true)
// и повторный запуск ничего не дублирует
@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = "app.data-seeder.enabled=true")
class DataSeederTest {

    @Autowired private DataSeeder seeder;

    @Autowired private com.example.eduplatform.repository.UserRepository userRepository;
    @Autowired private com.example.eduplatform.repository.CourseRepository courseRepository;
    @Autowired private com.example.eduplatform.repository.ModuleRepository moduleRepository;
    @Autowired private com.example.eduplatform.repository.LessonRepository lessonRepository;
    @Autowired private com.example.eduplatform.repository.AssignmentRepository assignmentRepository;
    @Autowired private com.example.eduplatform.repository.QuizRepository quizRepository;
    @Autowired private com.example.eduplatform.repository.EnrollmentRepository enrollmentRepository;
    @Autowired private com.example.eduplatform.repository.SubmissionRepository submissionRepository;
    @Autowired private com.example.eduplatform.repository.QuizSubmissionRepository quizSubmissionRepository;
    @Autowired private com.example.eduplatform.repository.CourseReviewRepository reviewRepository;
    @Autowired private com.example.eduplatform.repository.TagRepository tagRepository;
    @Autowired private com.example.eduplatform.repository.NotificationRepository notificationRepository;

    @Test
    void demoDataIsSeededAtStartup() {
        assertThat(userRepository.count()).isGreaterThanOrEqualTo(6);
        assertThat(courseRepository.count()).isGreaterThanOrEqualTo(3);
        assertThat(moduleRepository.count()).isGreaterThanOrEqualTo(4);
        assertThat(lessonRepository.count()).isGreaterThanOrEqualTo(7);
        assertThat(assignmentRepository.count()).isGreaterThanOrEqualTo(3);
        assertThat(quizRepository.count()).isGreaterThanOrEqualTo(1);
        assertThat(enrollmentRepository.count()).isGreaterThanOrEqualTo(4);
        assertThat(submissionRepository.count()).isGreaterThanOrEqualTo(3);
        assertThat(quizSubmissionRepository.count()).isGreaterThanOrEqualTo(2);
        assertThat(reviewRepository.count()).isGreaterThanOrEqualTo(2);
        assertThat(tagRepository.count()).isGreaterThanOrEqualTo(6);
        assertThat(notificationRepository.count()).isGreaterThanOrEqualTo(4);
    }

    @Test
    void repeatedSeedingIsIdempotent() {
        long usersBefore = userRepository.count();
        long coursesBefore = courseRepository.count();

        // повторный вызов пропускается, в базе уже есть данные
        seeder.run();

        assertThat(userRepository.count()).isEqualTo(usersBefore);
        assertThat(courseRepository.count()).isEqualTo(coursesBefore);
    }
}
