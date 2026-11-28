package com.example.eduplatform.service;

import com.example.eduplatform.dto.CreateUserRequest;
import com.example.eduplatform.dto.EnrollmentResponse;
import com.example.eduplatform.dto.NotificationResponse;
import com.example.eduplatform.dto.ProfileRequest;
import com.example.eduplatform.dto.ProfileResponse;
import com.example.eduplatform.dto.QuizSubmissionResponse;
import com.example.eduplatform.dto.SubmissionResponse;
import com.example.eduplatform.dto.UpdateUserRequest;
import com.example.eduplatform.dto.UserDetailResponse;
import com.example.eduplatform.dto.UserResponse;
import com.example.eduplatform.entity.Profile;
import com.example.eduplatform.entity.User;
import com.example.eduplatform.entity.enums.UserRole;
import com.example.eduplatform.exception.ConflictException;
import com.example.eduplatform.exception.DuplicateResourceException;
import com.example.eduplatform.exception.NotFoundException;
import com.example.eduplatform.repository.CourseRepository;
import com.example.eduplatform.repository.CourseReviewRepository;
import com.example.eduplatform.repository.EnrollmentRepository;
import com.example.eduplatform.repository.NotificationRepository;
import com.example.eduplatform.repository.ProfileRepository;
import com.example.eduplatform.repository.QuizAnswerRepository;
import com.example.eduplatform.repository.QuizSubmissionRepository;
import com.example.eduplatform.repository.SubmissionRepository;
import com.example.eduplatform.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

// юзеры: crud, профили, выборки активности студента
// сервис это граница транзакций, все ленивые связи читаются внутри его методов
@Service
@Transactional(readOnly = true)
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;
    private final ProfileRepository profileRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final SubmissionRepository submissionRepository;
    private final QuizSubmissionRepository quizSubmissionRepository;
    private final QuizAnswerRepository quizAnswerRepository;
    private final CourseReviewRepository reviewRepository;
    private final NotificationRepository notificationRepository;
    private final CourseRepository courseRepository;
    private final NotificationService notificationService;

    public UserService(UserRepository userRepository,
                       ProfileRepository profileRepository,
                       EnrollmentRepository enrollmentRepository,
                       SubmissionRepository submissionRepository,
                       QuizSubmissionRepository quizSubmissionRepository,
                       QuizAnswerRepository quizAnswerRepository,
                       CourseReviewRepository reviewRepository,
                       NotificationRepository notificationRepository,
                       CourseRepository courseRepository,
                       NotificationService notificationService) {
        this.userRepository = userRepository;
        this.profileRepository = profileRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.submissionRepository = submissionRepository;
        this.quizSubmissionRepository = quizSubmissionRepository;
        this.quizAnswerRepository = quizAnswerRepository;
        this.reviewRepository = reviewRepository;
        this.notificationRepository = notificationRepository;
        this.courseRepository = courseRepository;
        this.notificationService = notificationService;
    }

    public List<UserResponse> findAll() {
        return userRepository.findAll().stream().map(UserService::toResponse).toList();
    }

    public UserResponse findById(Long id) {
        return toResponse(getUser(id));
    }

    public UserDetailResponse findDetailById(Long id) {
        User user = userRepository.findWithProfileById(id)
                .orElseThrow(() -> new NotFoundException("Пользователь с id=" + id + " не найден"));
        ProfileResponse profile = user.getProfile() == null ? null : toProfileResponse(user.getProfile());
        return new UserDetailResponse(user.getId(), user.getName(), user.getEmail(), user.getRole(),
                user.isActive(), user.getCreatedAt(), profile);
    }

    @Transactional
    public UserResponse create(CreateUserRequest request) {
        if (userRepository.existsByEmail(request.email().toLowerCase())) {
            throw new DuplicateResourceException("Пользователь с email '" + request.email() + "' уже существует");
        }
        User user = new User();
        user.setName(request.name());
        user.setEmail(request.email().toLowerCase());
        user.setRole(request.role());
        User saved = userRepository.save(user);
        log.info("Создан пользователь: id={}, email={}, роль={}", saved.getId(), saved.getEmail(), saved.getRole());
        return toResponse(saved);
    }

    @Transactional
    public UserResponse update(Long id, UpdateUserRequest request) {
        User user = getUser(id);
        if (request.name() != null) {
            user.setName(request.name());
        }
        if (request.email() != null && !request.email().equalsIgnoreCase(user.getEmail())) {
            String newEmail = request.email().toLowerCase();
            userRepository.findByEmail(newEmail)
                    .filter(other -> !other.getId().equals(id))
                    .ifPresent(other -> {
                        throw new DuplicateResourceException("Email '" + newEmail + "' уже занят другим пользователем");
                    });
            user.setEmail(newEmail);
        }
        if (request.role() != null) {
            user.setRole(request.role());
        }
        if (request.active() != null) {
            user.setActive(request.active());
        }
        log.info("Обновлён пользователь id={}", id);
        return toResponse(user);
    }

    // удаление юзера вместе со всей его активностью.
    @Transactional
    public void delete(Long id) {
        User user = getUser(id);
        if (user.getRole() == UserRole.TEACHER && courseRepository.existsByTeacherId(id)) {
            throw new ConflictException("Нельзя удалить преподавателя '" + user.getName()
                    + "': за ним закреплено курсов — " + courseRepository.findByTeacherId(id).size()
                    + ". Сначала переназначьте курсы другим преподавателям.");
        }
        reviewRepository.deleteByStudentId(id);
        notificationRepository.deleteByRecipientId(id);
        quizAnswerRepository.deleteByStudentId(id);
        quizSubmissionRepository.deleteByStudentId(id);
        submissionRepository.deleteByStudentId(id);
        enrollmentRepository.deleteByStudentId(id);
        // профиль 1-1 удаляется каскадом вместе с юзером, его строка еще на месте
        userRepository.delete(user);
        log.info("Удалён пользователь id={}: вместе с ним удалены его записи на курсы, решения, "
                + "результаты тестов, отзывы, уведомления и профиль", id);
    }

    public ProfileResponse getProfile(Long userId) {
        getUser(userId);
        Profile profile = profileRepository.findByUserId(userId)
                .orElseThrow(() -> new NotFoundException("Профиль пользователя id=" + userId + " не найден"));
        return toProfileResponse(profile);
    }

    @Transactional
    public ProfileResponse updateProfile(Long userId, ProfileRequest request) {
        User user = getUser(userId);
        Profile profile = user.getProfile();
        if (profile == null) {
            profile = new Profile();
            profile.setUser(user);
            user.setProfile(profile);
        }
        profile.setBio(request.bio());
        profile.setAvatarUrl(request.avatarUrl());
        profile.setPhoneNumber(request.phoneNumber());
        profile.setCity(request.city());
        profile.setBirthDate(request.birthDate());
        profileRepository.save(profile);
        log.info("Сохранён профиль пользователя id={}", userId);
        return toProfileResponse(profile);
    }

    // курсы студента
    public List<EnrollmentResponse> getCourses(Long userId) {
        getUser(userId);
        return enrollmentRepository.findWithCourseByStudentId(userId).stream()
                .map(EnrollmentService::toResponse)
                .toList();
    }

    // все решения студента
    public List<SubmissionResponse> getSubmissions(Long userId) {
        getUser(userId);
        return submissionRepository.findWithDetailsByStudentId(userId).stream()
                .map(SubmissionService::toResponse)
                .toList();
    }

    // результаты тестов студента
    public List<QuizSubmissionResponse> getQuizResults(Long userId) {
        getUser(userId);
        return quizSubmissionRepository.findByStudentId(userId).stream()
                .map(QuizService::toResponse)
                .toList();
    }

    // уведомления юзера
    public List<NotificationResponse> getNotifications(Long userId) {
        getUser(userId);
        return notificationService.forUser(userId);
    }

    private User getUser(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Пользователь с id=" + id + " не найден"));
    }

    // маппинг в dto

    public static UserResponse toResponse(User user) {
        return new UserResponse(user.getId(), user.getName(), user.getEmail(), user.getRole(),
                user.isActive(), user.getCreatedAt());
    }

    public static ProfileResponse toProfileResponse(Profile profile) {
        return new ProfileResponse(profile.getId(), profile.getUser().getId(), profile.getBio(),
                profile.getAvatarUrl(), profile.getPhoneNumber(), profile.getCity(), profile.getBirthDate());
    }
}
