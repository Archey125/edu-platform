package com.example.eduplatform.support;

import com.example.eduplatform.entity.AnswerOption;
import com.example.eduplatform.entity.Assignment;
import com.example.eduplatform.entity.Category;
import com.example.eduplatform.entity.Course;
import com.example.eduplatform.entity.Enrollment;
import com.example.eduplatform.entity.Lesson;
import com.example.eduplatform.entity.Module;
import com.example.eduplatform.entity.Question;
import com.example.eduplatform.entity.Quiz;
import com.example.eduplatform.entity.Submission;
import com.example.eduplatform.entity.Tag;
import com.example.eduplatform.entity.User;
import com.example.eduplatform.entity.enums.QuestionType;
import com.example.eduplatform.entity.enums.SubmissionStatus;
import com.example.eduplatform.entity.enums.UserRole;
import com.example.eduplatform.repository.AssignmentRepository;
import com.example.eduplatform.repository.CategoryRepository;
import com.example.eduplatform.repository.CourseRepository;
import com.example.eduplatform.repository.EnrollmentRepository;
import com.example.eduplatform.repository.LessonRepository;
import com.example.eduplatform.repository.ModuleRepository;
import com.example.eduplatform.repository.QuizRepository;
import com.example.eduplatform.repository.SubmissionRepository;
import com.example.eduplatform.repository.TagRepository;
import com.example.eduplatform.repository.UserRepository;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicLong;

// фабрика тестовых данных, создает связанные сущности с уникальными именами/email,
// чтобы тесты не конфликтовали по уникальным индексам
@Component
public class TestDataFactory {

    private static final AtomicLong SEQ = new AtomicLong(System.currentTimeMillis() % 1_000_000);

    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final TagRepository tagRepository;
    private final CourseRepository courseRepository;
    private final ModuleRepository moduleRepository;
    private final LessonRepository lessonRepository;
    private final AssignmentRepository assignmentRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final QuizRepository quizRepository;
    private final SubmissionRepository submissionRepository;

    public TestDataFactory(UserRepository userRepository,
                           CategoryRepository categoryRepository,
                           TagRepository tagRepository,
                           CourseRepository courseRepository,
                           ModuleRepository moduleRepository,
                           LessonRepository lessonRepository,
                           AssignmentRepository assignmentRepository,
                           EnrollmentRepository enrollmentRepository,
                           QuizRepository quizRepository,
                           SubmissionRepository submissionRepository) {
        this.userRepository = userRepository;
        this.categoryRepository = categoryRepository;
        this.tagRepository = tagRepository;
        this.courseRepository = courseRepository;
        this.moduleRepository = moduleRepository;
        this.lessonRepository = lessonRepository;
        this.assignmentRepository = assignmentRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.quizRepository = quizRepository;
        this.submissionRepository = submissionRepository;
    }

    public User student() {
        return user(UserRole.STUDENT);
    }

    public User teacher() {
        return user(UserRole.TEACHER);
    }

    public User user(UserRole role) {
        long n = SEQ.incrementAndGet();
        User user = new User();
        user.setName("Пользователь " + n);
        user.setEmail("user" + n + "@test.io");
        user.setRole(role);
        return userRepository.save(user);
    }

    public Category category() {
        long n = SEQ.incrementAndGet();
        return categoryRepository.save(new Category("Категория " + n, "Тестовая категория"));
    }

    public Tag tag() {
        long n = SEQ.incrementAndGet();
        return tagRepository.save(new Tag("Тег" + n));
    }

    public Course course(User teacher, Category category) {
        long n = SEQ.incrementAndGet();
        Course course = new Course();
        course.setTitle("Курс " + n);
        course.setDescription("Тестовый курс " + n);
        course.setCategory(category);
        course.setTeacher(teacher);
        course.setDurationHours(10);
        return courseRepository.save(course);
    }

    public Module module(Course course) {
        Module module = new Module();
        module.setTitle("Модуль курса " + course.getId());
        module.setOrderIndex(course.getModules().size() + 1);
        course.addModule(module);
        return moduleRepository.save(module);
    }

    public Lesson lesson(Module module) {
        Lesson lesson = new Lesson();
        lesson.setTitle("Урок модуля " + module.getId());
        lesson.setContent("Тестовое содержимое урока");
        lesson.setOrderIndex(module.getLessons().size() + 1);
        module.addLesson(lesson);
        return lessonRepository.save(lesson);
    }

    public Assignment assignment(Lesson lesson) {
        return assignment(lesson, LocalDateTime.now().plusDays(7));
    }

    public Assignment assignment(Lesson lesson, LocalDateTime dueDate) {
        Assignment assignment = new Assignment();
        assignment.setTitle("Задание урока " + lesson.getId());
        assignment.setDescription("Тестовое задание");
        assignment.setDueDate(dueDate);
        assignment.setMaxScore(100);
        lesson.addAssignment(assignment);
        return assignmentRepository.save(assignment);
    }

    public Enrollment enroll(User student, Course course) {
        return enrollmentRepository.save(new Enrollment(student, course));
    }

    // тест с двумя вопросами: single choice (правильный "Верный ответ А")
    // и multiple (правильные "Множественный 1" и "Множественный 2")
    public Quiz quizWithMixedQuestions(Module module) {
        Quiz quiz = new Quiz();
        quiz.setTitle("Тест модуля " + module.getId());
        quiz.setTimeLimitMinutes(15);
        quiz.setModule(module);

        Question single = new Question();
        single.setQuestionText("Вопрос с одним ответом");
        single.setType(QuestionType.SINGLE_CHOICE);
        single.setPoints(1);
        single.addOption(new AnswerOption("Верный ответ А", true));
        single.addOption(new AnswerOption("Неверный ответ Б", false));
        quiz.addQuestion(single);

        Question multiple = new Question();
        multiple.setQuestionText("Вопрос с несколькими ответами");
        multiple.setType(QuestionType.MULTIPLE_CHOICE);
        multiple.setPoints(1);
        multiple.addOption(new AnswerOption("Множественный 1", true));
        multiple.addOption(new AnswerOption("Множественный 2", true));
        multiple.addOption(new AnswerOption("Множественный неверный", false));
        quiz.addQuestion(multiple);

        return quizRepository.save(quiz);
    }

    public Submission submission(Assignment assignment, User student) {
        Submission submission = new Submission();
        submission.setAssignment(assignment);
        submission.setStudent(student);
        assignment.addSubmission(submission);
        submission.setContent("Тестовое решение");
        submission.setStatus(SubmissionStatus.SUBMITTED);
        return submissionRepository.save(submission);
    }

    public AnswerOption optionByText(Question question, String text) {
        return question.getOptions().stream()
                .filter(o -> o.getOptionText().equals(text))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Нет варианта: " + text));
    }

    public Question questionByText(Quiz quiz, String text) {
        return quiz.getQuestions().stream()
                .filter(q -> q.getQuestionText().equals(text))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Нет вопроса: " + text));
    }
}
