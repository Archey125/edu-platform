package com.example.eduplatform.config;

import com.example.eduplatform.entity.AnswerOption;
import com.example.eduplatform.entity.Assignment;
import com.example.eduplatform.entity.Category;
import com.example.eduplatform.entity.Course;
import com.example.eduplatform.entity.CourseReview;
import com.example.eduplatform.entity.Enrollment;
import com.example.eduplatform.entity.Lesson;
import com.example.eduplatform.entity.Module;
import com.example.eduplatform.entity.Question;
import com.example.eduplatform.entity.Quiz;
import com.example.eduplatform.entity.QuizAnswer;
import com.example.eduplatform.entity.QuizSubmission;
import com.example.eduplatform.entity.Submission;
import com.example.eduplatform.entity.Tag;
import com.example.eduplatform.entity.User;
import com.example.eduplatform.entity.enums.NotificationType;
import com.example.eduplatform.entity.enums.QuestionType;
import com.example.eduplatform.entity.enums.SubmissionStatus;
import com.example.eduplatform.entity.enums.UserRole;
import com.example.eduplatform.repository.AnswerOptionRepository;
import com.example.eduplatform.repository.AssignmentRepository;
import com.example.eduplatform.repository.CategoryRepository;
import com.example.eduplatform.repository.CourseRepository;
import com.example.eduplatform.repository.CourseReviewRepository;
import com.example.eduplatform.repository.EnrollmentRepository;
import com.example.eduplatform.repository.LessonRepository;
import com.example.eduplatform.repository.ModuleRepository;
import com.example.eduplatform.repository.NotificationRepository;
import com.example.eduplatform.repository.QuizRepository;
import com.example.eduplatform.repository.QuestionRepository;
import com.example.eduplatform.repository.QuizSubmissionRepository;
import com.example.eduplatform.repository.SubmissionRepository;
import com.example.eduplatform.repository.TagRepository;
import com.example.eduplatform.repository.UserRepository;
import com.example.eduplatform.service.QuizScorer;
import com.example.eduplatform.service.QuizService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// заполнение базы демо-данными при старте, включается app.data-seeder.enabled=true (профиль dev)
// создает категории, теги, юзеров, курсы с модулями/уроками/заданиями/тестами,
// записи, решения с оценками, результаты тестов, отзывы и уведомления
@Component
@ConditionalOnProperty(name = "app.data-seeder.enabled", havingValue = "true")
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final TagRepository tagRepository;
    private final CourseRepository courseRepository;
    private final ModuleRepository moduleRepository;
    private final LessonRepository lessonRepository;
    private final AssignmentRepository assignmentRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final SubmissionRepository submissionRepository;
    private final QuizRepository quizRepository;
    private final QuestionRepository questionRepository;
    private final AnswerOptionRepository answerOptionRepository;
    private final QuizSubmissionRepository quizSubmissionRepository;
    private final CourseReviewRepository reviewRepository;
    private final NotificationRepository notificationRepository;

    public DataSeeder(UserRepository userRepository,
                      CategoryRepository categoryRepository,
                      TagRepository tagRepository,
                      CourseRepository courseRepository,
                      ModuleRepository moduleRepository,
                      LessonRepository lessonRepository,
                      AssignmentRepository assignmentRepository,
                      EnrollmentRepository enrollmentRepository,
                      SubmissionRepository submissionRepository,
                      QuizRepository quizRepository,
                      QuestionRepository questionRepository,
                      AnswerOptionRepository answerOptionRepository,
                      QuizSubmissionRepository quizSubmissionRepository,
                      CourseReviewRepository reviewRepository,
                      NotificationRepository notificationRepository) {
        this.userRepository = userRepository;
        this.categoryRepository = categoryRepository;
        this.tagRepository = tagRepository;
        this.courseRepository = courseRepository;
        this.moduleRepository = moduleRepository;
        this.lessonRepository = lessonRepository;
        this.assignmentRepository = assignmentRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.submissionRepository = submissionRepository;
        this.quizRepository = quizRepository;
        this.questionRepository = questionRepository;
        this.answerOptionRepository = answerOptionRepository;
        this.quizSubmissionRepository = quizSubmissionRepository;
        this.reviewRepository = reviewRepository;
        this.notificationRepository = notificationRepository;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (userRepository.count() > 0) {
            log.info("DataSeeder: база уже содержит данные — предзаполнение пропущено");
            return;
        }
        log.info("DataSeeder: заполняем базу демонстрационными данными...");
        seed();
        log.info("DataSeeder готов: пользователей={}, курсов={}, модулей={}, уроков={}, заданий={}, тестов={}, "
                        + "записей={}, решений={}, результатов тестов={}, отзывов={}, тегов={}, уведомлений={}",
                userRepository.count(), courseRepository.count(), moduleRepository.count(),
                lessonRepository.count(), assignmentRepository.count(), quizRepository.count(),
                enrollmentRepository.count(), submissionRepository.count(), quizSubmissionRepository.count(),
                reviewRepository.count(), tagRepository.count(), notificationRepository.count());
    }

    private void seed() {
        // категории и теги
        Category programming = categoryRepository.save(new Category("Программирование", "Курсы по разработке ПО"));
        Category databases = categoryRepository.save(new Category("Базы данных", "SQL, NoSQL и администрирование"));
        categoryRepository.save(new Category("Дизайн", "UX/UI и графический дизайн"));

        List<Tag> tags = List.of(
                tagRepository.save(new Tag("Java")),
                tagRepository.save(new Tag("Hibernate")),
                tagRepository.save(new Tag("Spring")),
                tagRepository.save(new Tag("PostgreSQL")),
                tagRepository.save(new Tag("Beginner")),
                tagRepository.save(new Tag("Advanced")));

        // пользователи
        User admin = user("Анна Волкова", "admin@edu.io", UserRole.ADMIN);
        User igor = user("Игорь Смирнов", "igor.smirnov@edu.io", UserRole.TEACHER);
        User elena = user("Елена Петрова", "elena.petrova@edu.io", UserRole.TEACHER);
        User maksim = user("Максим Козлов", "maksim@edu.io", UserRole.STUDENT);
        User olga = user("Ольга Новикова", "olga@edu.io", UserRole.STUDENT);
        User ivan = user("Иван Соколов", "ivan@edu.io", UserRole.STUDENT);

        profile(igor, "Преподаватель Java и Hibernate, 12 лет опыта", "Москва");
        profile(elena, "DBA и преподаватель SQL", "Санкт-Петербург");
        profile(maksim, "Начинающий backend-разработчик", "Казань");

        // курс 1: ORM-фреймворки для Java
        Course ormCourse = course("ORM-фреймворки для Java",
                "Практический курс по JPA/Hibernate: связи сущностей, ленивая загрузка, репозитории Spring Data.",
                programming, igor, 40, LocalDate.now().plusDays(14));
        ormCourse.addTag(tags.get(0));
        ormCourse.addTag(tags.get(1));
        ormCourse.addTag(tags.get(2));

        Module module1 = module(ormCourse, "Введение в ORM", "Модели данных, EntityManager, контекст персистентности", 1);
        Lesson lesson1 = lesson(module1, "Что такое ORM и зачем он нужен",
                "ORM (Object-Relational Mapping) — отображение объектов на таблицы БД...", null, 1);
        lesson(module1, "JPA: EntityManager и контекст персистентности",
                "Контекст персистентности — «память» Hibernate между транзакциями...", null, 2);
        Assignment assignment1 = assignment(lesson1, "Настройка проекта с Hibernate",
                "Создайте Spring Boot проект, подключите PostgreSQL и опишите первую сущность.",
                LocalDateTime.now().plusDays(7), 100);
        Quiz quiz1 = quiz(module1, "Тест по модулю 1", 20);

        Module module2 = module(ormCourse, "Связи сущностей", "OneToMany, ManyToOne, ManyToMany, OneToOne", 2);
        Lesson lesson3 = lesson(module2, "OneToMany и ManyToOne",
                "Связь «один ко многим»: у одного курса много модулей...", null, 1);
        lesson(module2, "ManyToMany и промежуточные сущности",
                "Когда нужен не просто join-таблица, а отдельная сущность-связка...", null, 2);
        Assignment assignment2 = assignment(lesson3, "Смоделируйте связи учебной платформы",
                "Опишите сущности User, Course, Enrollment и их связи.", LocalDateTime.now().plusDays(10), 100);

        // курс 2: Основы PostgreSQL
        Course pgCourse = course("Основы PostgreSQL",
                "Базовый курс по PostgreSQL: выборки, соединения, индексы и транзакции.",
                databases, elena, 24, LocalDate.now().plusDays(7));
        pgCourse.addTag(tags.get(3));
        pgCourse.addTag(tags.get(4));

        Module pgModule = module(pgCourse, "Введение в SQL", "Базовые запросы SELECT", 1);
        Lesson pgLesson1 = lesson(pgModule, "SELECT — выборка данных",
                "SELECT * FROM users; — базовая выборка...", null, 1);
        lesson(pgModule, "JOIN — соединения таблиц",
                "INNER JOIN, LEFT JOIN и другие типы соединений...", null, 2);
        Assignment pgAssignment = assignment(pgLesson1, "Напишите пять запросов",
                "Составьте запросы выборки с фильтрацией и сортировкой.", LocalDateTime.now().plusDays(5), 50);

        // курс 3: Дизайн
        Course uxCourse = course("UX-основы дизайна интерфейсов",
                "Пользовательские сценарии, прототипирование и юзабилити-аудит.",
                categoryRepository.findByName("Дизайн").orElseThrow(), elena, 16, null);
        Module uxModule = module(uxCourse, "Принципы UX", "Как думать о пользователе", 1);
        lesson(uxModule, "Пользовательские сценарии", "CJM и user flow...", null, 1);

        // записи на курсы
        Enrollment eMaxOrm = enroll(maksim, ormCourse);
        Enrollment eOlgaOrm = enroll(olga, ormCourse);
        Enrollment eMaxPg = enroll(maksim, pgCourse);
        Enrollment eIvanPg = enroll(ivan, pgCourse);

        // решения и оценки
        submission(maksim, assignment1, "Проект собран: spring-boot-starter-data-jpa + драйвер PostgreSQL. "
                + "Сущность User описана, schema создана автоматически.", null, SubmissionStatus.SUBMITTED, null, null);
        submission(olga, assignment1, "Настроила проект, сущность создана, добавила application.yml с параметрами БД.",
                null, SubmissionStatus.GRADED, 90, "Отличная работа! Рекомендую изучить профили конфигурации.");
        submission(ivan, pgAssignment, "Пять запросов: SELECT с WHERE, ORDER BY, LIMIT, JOIN и GROUP BY.",
                null, SubmissionStatus.GRADED, 70, "Запросы верные, но добавьте пояснения к GROUP BY.");

        // тест модуля 1: вопросы и варианты
        Question q1 = question(quiz1, "Какая аннотация задаёт первичный ключ сущности?", QuestionType.SINGLE_CHOICE, 4);
        option(q1, "@Id", true);
        option(q1, "@PrimaryKey", false);
        option(q1, "@Key", false);
        Question q2 = question(quiz1, "Какой режим загрузки у @OneToMany по умолчанию в JPA?", QuestionType.SINGLE_CHOICE, 4);
        option(q2, "LAZY", true);
        option(q2, "EAGER", false);
        option(q2, "AUTO", false);
        Question q3 = question(quiz1, "Какие способы решить проблему LazyInitializationException существуют?",
                QuestionType.MULTIPLE_CHOICE, 4);
        option(q3, "JOIN FETCH в JPQL-запросе", true);
        option(q3, "@EntityGraph на методе репозитория", true);
        option(q3, "Читать коллекцию внутри транзакции", true);
        option(q3, "Удалить связь между сущностями", false);

        // результаты тестов
        takeQuiz(maksim, quiz1, Map.of(
                q1.getId(), List.of(correctOptionId(q1)),
                q2.getId(), List.of(correctOptionId(q2)),
                q3.getId(), List.of(firstCorrectOptionId(q3), thirdCorrectOptionId(q3))));
        takeQuiz(olga, quiz1, Map.of(
                q1.getId(), List.of(correctOptionId(q1)),
                q2.getId(), List.of(wrongOptionId(q2)),
                q3.getId(), List.of(firstCorrectOptionId(q3), lastWrongOptionId(q3))));

        // отзывы
        review(olga, ormCourse, 5, "Отличный курс: много практики, понятно объясняется ленивая загрузка.");
        review(maksim, ormCourse, 4, "Материал хороший, хотелось бы больше заданий на ManyToMany.");

        // уведомления
        notification(igor, NotificationType.ENROLLMENT, "Студент Максим Козлов записался на курс «ORM-фреймворки для Java»");
        notification(igor, NotificationType.SUBMISSION_RECEIVED, "Студент Максим Козлов отправил решение по заданию «Настройка проекта с Hibernate»");
        notification(olga, NotificationType.SUBMISSION_GRADED, "Задание «Настройка проекта с Hibernate» проверено: 90 из 100");
        notification(maksim, NotificationType.QUIZ_COMPLETED, "Тест «Тест по модулю 1» пройден (зачёт)");
    }

    // хелперы

    private User user(String name, String email, UserRole role) {
        User user = new User();
        user.setName(name);
        user.setEmail(email);
        user.setRole(role);
        return userRepository.save(user);
    }

    private void profile(User user, String bio, String city) {
        com.example.eduplatform.entity.Profile profile = new com.example.eduplatform.entity.Profile();
        profile.setUser(user);
        user.setProfile(profile);
        profile.setBio(bio);
        profile.setCity(city);
    }

    private Course course(String title, String description, Category category, User teacher,
                          Integer durationHours, LocalDate startDate) {
        Course course = new Course();
        course.setTitle(title);
        course.setDescription(description);
        course.setCategory(category);
        course.setTeacher(teacher);
        course.setDurationHours(durationHours);
        course.setStartDate(startDate);
        return courseRepository.save(course);
    }

    private Module module(Course course, String title, String description, int orderIndex) {
        Module module = new Module();
        module.setTitle(title);
        module.setDescription(description);
        module.setOrderIndex(orderIndex);
        course.addModule(module);
        return moduleRepository.save(module);
    }

    private Lesson lesson(Module module, String title, String content, String videoUrl, int orderIndex) {
        Lesson lesson = new Lesson();
        lesson.setTitle(title);
        lesson.setContent(content);
        lesson.setVideoUrl(videoUrl);
        lesson.setOrderIndex(orderIndex);
        module.addLesson(lesson);
        return lessonRepository.save(lesson);
    }

    private Assignment assignment(Lesson lesson, String title, String description,
                                  LocalDateTime dueDate, Integer maxScore) {
        Assignment assignment = new Assignment();
        assignment.setTitle(title);
        assignment.setDescription(description);
        assignment.setDueDate(dueDate);
        assignment.setMaxScore(maxScore);
        lesson.addAssignment(assignment);
        return assignmentRepository.save(assignment);
    }

    private Quiz quiz(Module module, String title, Integer timeLimitMinutes) {
        Quiz quiz = new Quiz();
        quiz.setTitle(title);
        quiz.setTimeLimitMinutes(timeLimitMinutes);
        quiz.setModule(module);
        return quizRepository.save(quiz);
    }

    private Question question(Quiz quiz, String text, QuestionType type, int points) {
        Question question = new Question();
        question.setQuestionText(text);
        question.setType(type);
        question.setPoints(points);
        quiz.addQuestion(question);
        // persist без merge, чтобы вопрос сразу вставился и получил id
        questionRepository.save(question);
        return question;
    }

    private AnswerOption option(Question question, String text, boolean correct) {
        AnswerOption option = new AnswerOption(text, correct);
        question.addOption(option);
        answerOptionRepository.save(option);
        return option;
    }

    private Enrollment enroll(User student, Course course) {
        return enrollmentRepository.save(new Enrollment(student, course));
    }

    private void submission(User student, Assignment assignment, String content, String fileUrl,
                            SubmissionStatus status, Integer score, String feedback) {
        Submission submission = new Submission();
        submission.setStudent(student);
        submission.setAssignment(assignment);
        assignment.addSubmission(submission);
        submission.setContent(content);
        submission.setFileUrl(fileUrl);
        submission.setStatus(status);
        submission.setScore(score);
        submission.setFeedback(feedback);
        submissionRepository.save(submission);
    }

    private void takeQuiz(User student, Quiz quiz, Map<Long, List<Long>> answers) {
        QuizScorer.ScoreResult result = QuizScorer.calculate(quiz.getQuestions(), answers);
        QuizSubmission submission = new QuizSubmission();
        submission.setQuiz(quiz);
        submission.setStudent(student);
        submission.setScore(result.score());
        submission.setMaxScore(result.maxScore());
        submission.setPassed(result.maxScore() > 0
                && result.score() * 100 >= result.maxScore() * QuizService.PASS_PERCENT);
        for (Map.Entry<Long, List<Long>> entry : answers.entrySet()) {
            Question question = quiz.getQuestions().stream()
                    .filter(q -> q.getId().equals(entry.getKey()))
                    .findFirst()
                    .orElseThrow();
            for (Long optionId : entry.getValue()) {
                AnswerOption option = question.getOptions().stream()
                        .filter(o -> o.getId().equals(optionId))
                        .findFirst()
                        .orElseThrow();
                QuizAnswer quizAnswer = new QuizAnswer(question, option, option.isCorrect());
                submission.addAnswer(quizAnswer);
            }
        }
        quizSubmissionRepository.save(submission);
    }

    private Long correctOptionId(Question question) {
        return question.getOptions().stream()
                .filter(AnswerOption::isCorrect)
                .findFirst()
                .orElseThrow()
                .getId();
    }

    private Long wrongOptionId(Question question) {
        return question.getOptions().stream()
                .filter(o -> !o.isCorrect())
                .findFirst()
                .orElseThrow()
                .getId();
    }

    private Long firstCorrectOptionId(Question question) {
        return question.getOptions().stream()
                .filter(AnswerOption::isCorrect)
                .findFirst()
                .orElseThrow()
                .getId();
    }

    private Long thirdCorrectOptionId(Question question) {
        List<AnswerOption> correct = question.getOptions().stream()
                .filter(AnswerOption::isCorrect)
                .toList();
        return correct.get(Math.min(2, correct.size() - 1)).getId();
    }

    private Long lastWrongOptionId(Question question) {
        List<AnswerOption> wrong = question.getOptions().stream()
                .filter(o -> !o.isCorrect())
                .toList();
        return wrong.get(wrong.size() - 1).getId();
    }

    private void review(User student, Course course, int rating, String comment) {
        CourseReview review = new CourseReview();
        review.setCourse(course);
        review.setStudent(student);
        review.setRating(rating);
        review.setComment(comment);
        reviewRepository.save(review);
    }

    private void notification(User recipient, NotificationType type, String message) {
        notificationRepository.save(new com.example.eduplatform.entity.Notification(recipient, type, message));
    }
}
