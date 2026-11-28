package com.example.eduplatform.integration;

import com.example.eduplatform.entity.AnswerOption;
import com.example.eduplatform.entity.Assignment;
import com.example.eduplatform.entity.Course;
import com.example.eduplatform.entity.Lesson;
import com.example.eduplatform.entity.Module;
import com.example.eduplatform.entity.Question;
import com.example.eduplatform.entity.Quiz;
import com.example.eduplatform.entity.Submission;
import com.example.eduplatform.entity.User;
import com.example.eduplatform.entity.enums.UserRole;
import com.example.eduplatform.repository.AssignmentRepository;
import com.example.eduplatform.repository.CourseRepository;
import com.example.eduplatform.repository.LessonRepository;
import com.example.eduplatform.repository.ModuleRepository;
import com.example.eduplatform.repository.QuestionRepository;
import com.example.eduplatform.repository.SubmissionRepository;
import com.example.eduplatform.repository.UserRepository;
import com.example.eduplatform.support.TestDataFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// проверка что схема бд создается по jpa моделям и базовый маппинг работает:
// генерация ключей, каскадное сохранение графа, уникальные индексы, enum как строка
@SpringBootTest
@ActiveProfiles("test")
class SchemaAndEntitiesTest {

    @Autowired private UserRepository userRepository;
    @Autowired private CourseRepository courseRepository;
    @Autowired private ModuleRepository moduleRepository;
    @Autowired private LessonRepository lessonRepository;
    @Autowired private AssignmentRepository assignmentRepository;
    @Autowired private SubmissionRepository submissionRepository;
    @Autowired private QuestionRepository questionRepository;
    @Autowired private TestDataFactory factory;
    @Autowired private JdbcTemplate jdbcTemplate;

    @Test
    void contextLoads() {
        // контекст поднялся, репозитории на месте - значит схема бд создана
        assertThat(userRepository).isNotNull();
        assertThat(courseRepository).isNotNull();
        assertThat(factory).isNotNull();
    }

    @Test
    void schemaContainsAllEntityTables() {
        // собираю имена всех таблиц (регистр зависит от режима h2) и сверяю каждую сущность
        var tableNames = jdbcTemplate.queryForList("select table_name from information_schema.tables", String.class)
                .stream()
                .map(String::toLowerCase)
                .collect(java.util.stream.Collectors.toSet());
        var expected = java.util.List.of(
                "users", "profiles", "categories", "courses", "enrollments", "modules", "lessons",
                "assignments", "submissions", "quizzes", "questions", "answer_options",
                "quiz_submissions", "quiz_answers", "course_reviews", "tags", "course_tags", "notifications");
        assertThat(tableNames).containsAll(expected);
    }

    @Test
    void cascadePersistSavesWholeGraph() {
        User teacher = factory.teacher();
        com.example.eduplatform.entity.Category category = factory.category();

        Course course = new Course();
        course.setTitle("Курс с полной структурой");
        course.setCategory(category);
        course.setTeacher(teacher);

        Module module = new Module();
        module.setTitle("Модуль 1");
        module.setOrderIndex(1);
        course.addModule(module);

        Lesson lesson = new Lesson();
        lesson.setTitle("Урок 1");
        lesson.setOrderIndex(1);
        module.addLesson(lesson);

        Assignment assignment = new Assignment();
        assignment.setTitle("Задание 1");
        assignment.setMaxScore(100);
        lesson.addAssignment(assignment);

        // сохраняю только курс, каскад должен сохранить весь граф
        Course saved = courseRepository.save(course);

        assertThat(saved.getId()).isNotNull();
        assertThat(module.getId()).isNotNull();
        assertThat(lesson.getId()).isNotNull();
        assertThat(assignment.getId()).isNotNull();
        assertThat(moduleRepository.findById(module.getId())).isPresent();
        assertThat(lessonRepository.findById(lesson.getId())).isPresent();
        assertThat(assignmentRepository.findById(assignment.getId())).isPresent();
    }

    @Test
    void submissionPersistsWithReferences() {
        User teacher = factory.teacher();
        Course course = factory.course(teacher, factory.category());
        Module module = factory.module(course);
        Lesson lesson = factory.lesson(module);
        Assignment assignment = factory.assignment(lesson);
        User student = factory.student();

        Submission submission = factory.submission(assignment, student);
        assertThat(submission.getId()).isNotNull();
        assertThat(submission.getSubmittedAt()).isNotNull();
        assertThat(submissionRepository.findById(submission.getId())).isPresent();
    }

    @Test
    void uniqueEmailIsEnforcedAtDbLevel() {
        User user = factory.student();
        User duplicate = new User();
        duplicate.setName("Дубль");
        duplicate.setEmail(user.getEmail());
        duplicate.setRole(UserRole.STUDENT);
        assertThatThrownBy(() -> userRepository.saveAndFlush(duplicate))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void enumsAreStoredAsStrings() {
        User teacher = factory.teacher();
        String role = jdbcTemplate.queryForObject(
                "select role from users where id = ?", String.class, teacher.getId());
        assertThat(role).isEqualTo("TEACHER");
    }

    @Test
    void quizQuestionsAndOptionsCascadeSaved() {
        User teacher = factory.teacher();
        Course course = factory.course(teacher, factory.category());
        Module module = factory.module(course);
        Quiz quiz = factory.quizWithMixedQuestions(module);

        assertThat(quiz.getId()).isNotNull();
        List<Question> questions = List.copyOf(quiz.getQuestions());
        assertThat(questions).hasSize(2);
        for (Question question : questions) {
            assertThat(question.getId()).isNotNull();
            for (AnswerOption option : question.getOptions()) {
                assertThat(option.getId()).isNotNull();
            }
        }
        assertThat(questionRepository.count()).isGreaterThanOrEqualTo(2);
    }
}
