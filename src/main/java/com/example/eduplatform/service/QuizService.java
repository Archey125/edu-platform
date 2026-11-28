package com.example.eduplatform.service;

import com.example.eduplatform.dto.CreateQuizRequest;
import com.example.eduplatform.dto.OptionRequest;
import com.example.eduplatform.dto.QuestionRequest;
import com.example.eduplatform.dto.QuizResponse;
import com.example.eduplatform.dto.QuizSubmissionResponse;
import com.example.eduplatform.dto.QuestionResponse;
import com.example.eduplatform.dto.OptionResponse;
import com.example.eduplatform.dto.TakeQuizRequest;
import com.example.eduplatform.entity.AnswerOption;
import com.example.eduplatform.entity.Course;
import com.example.eduplatform.entity.Enrollment;
import com.example.eduplatform.entity.Question;
import com.example.eduplatform.entity.Quiz;
import com.example.eduplatform.entity.QuizSubmission;
import com.example.eduplatform.entity.QuizAnswer;
import com.example.eduplatform.entity.User;
import com.example.eduplatform.entity.enums.EnrollmentStatus;
import com.example.eduplatform.entity.enums.NotificationType;
import com.example.eduplatform.entity.enums.QuestionType;
import com.example.eduplatform.entity.enums.UserRole;
import com.example.eduplatform.exception.BusinessRuleException;
import com.example.eduplatform.exception.DuplicateResourceException;
import com.example.eduplatform.exception.NotFoundException;
import com.example.eduplatform.repository.EnrollmentRepository;
import com.example.eduplatform.repository.ModuleRepository;
import com.example.eduplatform.repository.QuizRepository;
import com.example.eduplatform.repository.QuizSubmissionRepository;
import com.example.eduplatform.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

// тесты: создание теста модуля с вопросами, прохождение с автоподсчетом балла и сохранением попытки
@Service
@Transactional(readOnly = true)
public class QuizService {

    // сколько процентов правильных ответов нужно для зачета
    public static final int PASS_PERCENT = 60;

    private static final Logger log = LoggerFactory.getLogger(QuizService.class);

    private final QuizRepository quizRepository;
    private final ModuleRepository moduleRepository;
    private final QuizSubmissionRepository quizSubmissionRepository;
    private final UserRepository userRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final NotificationService notificationService;

    public QuizService(QuizRepository quizRepository,
                       ModuleRepository moduleRepository,
                       QuizSubmissionRepository quizSubmissionRepository,
                       UserRepository userRepository,
                       EnrollmentRepository enrollmentRepository,
                       NotificationService notificationService) {
        this.quizRepository = quizRepository;
        this.moduleRepository = moduleRepository;
        this.quizSubmissionRepository = quizSubmissionRepository;
        this.userRepository = userRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.notificationService = notificationService;
    }

    @Transactional
    public QuizResponse createForModule(Long moduleId, CreateQuizRequest request) {
        var module = moduleRepository.findById(moduleId)
                .orElseThrow(() -> new NotFoundException("Модуль с id=" + moduleId + " не найден"));
        if (quizRepository.findByModuleId(moduleId).isPresent()) {
            throw new DuplicateResourceException("У модуля '" + module.getTitle()
                    + "' уже есть тест: у одного модуля может быть только один тест");
        }
        Quiz quiz = new Quiz();
        quiz.setModule(module);
        quiz.setTitle(request.title());
        quiz.setTimeLimitMinutes(request.timeLimitMinutes());
        for (QuestionRequest questionRequest : request.questions()) {
            quiz.addQuestion(buildQuestion(questionRequest));
        }
        Quiz saved = quizRepository.save(quiz); // каскад сохранит вопросы и варианты
        log.info("Создан тест '{}' (id={}) для модуля id={}: вопросов — {}", saved.getTitle(), saved.getId(), moduleId, saved.getQuestions().size());
        return toResponse(saved);
    }

    @Transactional
    public QuestionResponse addQuestion(Long quizId, QuestionRequest request) {
        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new NotFoundException("Тест с id=" + quizId + " не найден"));
        Question question = buildQuestion(request);
        quiz.addQuestion(question);
        quizRepository.save(quiz);
        log.info("В тест id={} добавлен вопрос", quizId);
        return toQuestionResponse(question);
    }

    // тест с вопросами, правильные ответы наружу не отдаем
    public QuizResponse findById(Long id) {
        Quiz quiz = quizRepository.findWithQuestionsById(id)
                .orElseThrow(() -> new NotFoundException("Тест с id=" + id + " не найден"));
        return toResponse(quiz);
    }

    // прохождение теста: проверяем что студент записан на курс, что отвечено на все вопросы, считаем балл и сохраняем попытку с выбранными ответами
    @Transactional
    public QuizSubmissionResponse take(Long quizId, TakeQuizRequest request) {
        User student = userRepository.findById(request.studentId())
                .orElseThrow(() -> new NotFoundException("Пользователь с id=" + request.studentId() + " не найден"));
        if (student.getRole() != UserRole.STUDENT) {
            throw new BusinessRuleException("Пройти тест может только студент");
        }
        Quiz quiz = quizRepository.findWithQuestionsById(quizId)
                .orElseThrow(() -> new NotFoundException("Тест с id=" + quizId + " не найден"));
        if (quizSubmissionRepository.existsByQuizIdAndStudentId(quizId, student.getId())) {
            throw new DuplicateResourceException("Тест «" + quiz.getTitle()
                    + "» уже пройден этим студентом: повторная сдача не предусмотрена");
        }
        Course course = quiz.getModule().getCourse();
        Enrollment enrollment = enrollmentRepository
                .findByStudentIdAndCourseId(student.getId(), course.getId())
                .orElseThrow(() -> new BusinessRuleException("Тест доступен только студентам, записанным на курс «"
                        + course.getTitle() + "»"));
        if (enrollment.getStatus() != EnrollmentStatus.ACTIVE) {
            throw new BusinessRuleException("Запись на курс имеет статус " + enrollment.getStatus()
                    + " — прохождение теста недоступно");
        }
        validateAnswers(quiz.getQuestions(), request.answers());
        QuizScorer.ScoreResult result = QuizScorer.calculate(quiz.getQuestions(), request.answers());
        boolean passed = result.maxScore() > 0 && result.score() * 100 >= result.maxScore() * PASS_PERCENT;
        QuizSubmission submission = new QuizSubmission();
        submission.setQuiz(quiz);
        submission.setStudent(student);
        submission.setScore(result.score());
        submission.setMaxScore(result.maxScore());
        submission.setPassed(passed);
        for (Map.Entry<Long, List<Long>> entry : request.answers().entrySet()) {
            Question question = findQuestion(quiz.getQuestions(), entry.getKey());
            for (Long optionId : entry.getValue()) {
                AnswerOption option = findOption(question.getOptions(), optionId);
                submission.addAnswer(new QuizAnswer(question, option, option.isCorrect()));
            }
        }
        QuizSubmission saved = quizSubmissionRepository.save(submission);
        notificationService.notify(student, NotificationType.QUIZ_COMPLETED,
                "Тест «" + quiz.getTitle() + "» пройден: " + result.score() + " из " + result.maxScore()
                        + (passed ? " (зачёт)" : " (незачёт)"));
        log.info("Студент id={} прошёл тест id={}: {}/{} ({})", student.getId(), quizId,
                result.score(), result.maxScore(), passed ? "зачёт" : "незачёт");
        return toResponse(saved);
    }

    // результаты теста: все попытки или попытка конкретного студента
    public List<QuizSubmissionResponse> getResults(Long quizId, Long studentId) {
        List<QuizSubmission> submissions;
        if (studentId != null) {
            submissions = quizSubmissionRepository.findByQuizIdAndStudentId(quizId, studentId)
                    .map(List::of)
                    .orElse(List.of());
        } else {
            submissions = quizSubmissionRepository.findWithStudentByQuizId(quizId);
        }
        return submissions.stream().map(QuizService::toResponse).toList();
    }

    // проверка ответов

    private void validateAnswers(Set<Question> questions, Map<Long, List<Long>> answers) {
        for (Question question : questions) {
            List<Long> selected = answers.get(question.getId());
            if (selected == null || selected.isEmpty()) {
                throw new BusinessRuleException("Вопрос «" + question.getQuestionText() + "» остался без ответа");
            }
            if (question.getType() == QuestionType.SINGLE_CHOICE && selected.size() > 1) {
                throw new BusinessRuleException("Вопрос «" + question.getQuestionText()
                        + "» допускает только один вариант ответа");
            }
            Set<Long> optionIds = question.getOptions().stream()
                    .map(AnswerOption::getId)
                    .collect(Collectors.toSet());
            for (Long optionId : selected) {
                if (!optionIds.contains(optionId)) {
                    throw new BusinessRuleException("Вариант ответа id=" + optionId
                            + " не относится к вопросу «" + question.getQuestionText() + "»");
                }
            }
        }
        Set<Long> questionIds = questions.stream().map(Question::getId).collect(Collectors.toSet());
        for (Long askedQuestionId : answers.keySet()) {
            if (!questionIds.contains(askedQuestionId)) {
                throw new BusinessRuleException("Тест не содержит вопроса с id=" + askedQuestionId);
            }
        }
    }

    private Question buildQuestion(QuestionRequest request) {
        List<OptionRequest> options = request.options();
        if (options.size() < 2) {
            throw new BusinessRuleException("У вопроса должно быть минимум два варианта ответа");
        }
        long correctCount = options.stream().filter(OptionRequest::isCorrect).count();
        if (correctCount == 0) {
            throw new BusinessRuleException("Вопрос «" + request.questionText()
                    + "» должен иметь хотя бы один правильный вариант");
        }
        if (request.type() == QuestionType.SINGLE_CHOICE && correctCount > 1) {
            throw new BusinessRuleException("У вопроса «" + request.questionText()
                    + "» с типом SINGLE_CHOICE может быть только один правильный вариант");
        }
        Question question = new Question();
        question.setQuestionText(request.questionText());
        question.setType(request.type());
        question.setPoints(request.points() != null ? request.points() : 1);
        for (OptionRequest optionRequest : options) {
            question.addOption(new AnswerOption(optionRequest.optionText(), optionRequest.isCorrect()));
        }
        return question;
    }

    private Question findQuestion(Set<Question> questions, Long questionId) {
        return questions.stream()
                .filter(q -> q.getId().equals(questionId))
                .findFirst()
                .orElseThrow(() -> new NotFoundException("Вопрос с id=" + questionId + " не найден в тесте"));
    }

    private AnswerOption findOption(Set<AnswerOption> options, Long optionId) {
        return options.stream()
                .filter(o -> o.getId().equals(optionId))
                .findFirst()
                .orElseThrow(() -> new NotFoundException("Вариант ответа с id=" + optionId + " не найден"));
    }

    // маппинг в dto

    public static QuizResponse toResponse(Quiz quiz) {
        List<QuestionResponse> questions = quiz.getQuestions().stream()
                .sorted(Comparator.comparing(Question::getId))
                .map(QuizService::toQuestionResponse)
                .toList();
        return new QuizResponse(quiz.getId(), quiz.getModule().getId(), quiz.getTitle(),
                quiz.getTimeLimitMinutes(), questions);
    }

    public static QuestionResponse toQuestionResponse(Question question) {
        List<OptionResponse> options = question.getOptions().stream()
                .sorted(Comparator.comparing(AnswerOption::getId))
                .map(option -> new OptionResponse(option.getId(), option.getOptionText()))
                .toList();
        return new QuestionResponse(question.getId(), question.getQuestionText(), question.getType(),
                question.getPoints(), options);
    }

    public static QuizSubmissionResponse toResponse(QuizSubmission submission) {
        int percent = submission.getMaxScore() > 0
                ? Math.round(submission.getScore() * 100f / submission.getMaxScore())
                : 0;
        return new QuizSubmissionResponse(submission.getId(), submission.getQuiz().getId(),
                submission.getStudent().getId(), submission.getScore(), submission.getMaxScore(),
                percent, submission.isPassed(), submission.getTakenAt());
    }
}
