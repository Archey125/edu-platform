package com.example.eduplatform.service;

import com.example.eduplatform.dto.EnrollmentCreateRequest;
import com.example.eduplatform.dto.EnrollmentResponse;
import com.example.eduplatform.dto.EnrollmentUpdateRequest;
import com.example.eduplatform.entity.Course;
import com.example.eduplatform.entity.Enrollment;
import com.example.eduplatform.entity.User;
import com.example.eduplatform.entity.enums.NotificationType;
import com.example.eduplatform.entity.enums.UserRole;
import com.example.eduplatform.exception.BusinessRuleException;
import com.example.eduplatform.exception.DuplicateResourceException;
import com.example.eduplatform.exception.NotFoundException;
import com.example.eduplatform.repository.CourseRepository;
import com.example.eduplatform.repository.EnrollmentRepository;
import com.example.eduplatform.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

// запись студентов на курс, m2m user <-> course через enrollment
// с проверкой уникальности (дважды записаться нельзя) и статусов
@Service
@Transactional(readOnly = true)
public class EnrollmentService {

    private static final Logger log = LoggerFactory.getLogger(EnrollmentService.class);

    private final EnrollmentRepository enrollmentRepository;
    private final UserRepository userRepository;
    private final CourseRepository courseRepository;
    private final NotificationService notificationService;

    public EnrollmentService(EnrollmentRepository enrollmentRepository,
                             UserRepository userRepository,
                             CourseRepository courseRepository,
                             NotificationService notificationService) {
        this.enrollmentRepository = enrollmentRepository;
        this.userRepository = userRepository;
        this.courseRepository = courseRepository;
        this.notificationService = notificationService;
    }

    @Transactional
    public EnrollmentResponse enroll(Long courseId, Long userId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new NotFoundException("Курс с id=" + courseId + " не найден"));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователь с id=" + userId + " не найден"));
        if (user.getRole() != UserRole.STUDENT) {
            throw new BusinessRuleException("Записаться на курс может только студент (пользователь '"
                    + user.getName() + "' имеет роль " + user.getRole() + ")");
        }
        if (enrollmentRepository.existsByStudentIdAndCourseId(userId, courseId)) {
            throw new DuplicateResourceException("Студент '" + user.getName() + "' уже записан на курс '"
                    + course.getTitle() + "'");
        }
        Enrollment enrollment = new Enrollment(user, course);
        Enrollment saved = enrollmentRepository.save(enrollment);
        notificationService.notify(course.getTeacher(), NotificationType.ENROLLMENT,
                "Студент " + user.getName() + " записался на курс «" + course.getTitle() + "»");
        log.info("Студент '{}' (id={}) записан на курс '{}' (id={})", user.getName(), userId, course.getTitle(), courseId);
        return toResponse(saved);
    }

    @Transactional
    public EnrollmentResponse enroll(EnrollmentCreateRequest request) {
        return enroll(request.courseId(), request.userId());
    }

    public EnrollmentResponse findById(Long id) {
        return toResponse(getEnrollment(id));
    }

    // отписка, просто удаляем запись
    @Transactional
    public void unenroll(Long id) {
        Enrollment enrollment = getEnrollment(id);
        Long studentId = enrollment.getStudent().getId();
        Long courseId = enrollment.getCourse().getId();
        enrollmentRepository.delete(enrollment);
        log.info("Студент id={} отписан от курса id={} (запись id={} удалена)", studentId, courseId, id);
    }

    @Transactional
    public EnrollmentResponse updateStatus(Long id, EnrollmentUpdateRequest request) {
        Enrollment enrollment = getEnrollment(id);
        enrollment.setStatus(request.status());
        log.info("Запись id={} переведена в статус {}", id, request.status());
        return toResponse(enrollment);
    }

    // кто записан на курс
    public List<EnrollmentResponse> findByCourse(Long courseId) {
        return enrollmentRepository.findWithStudentByCourseId(courseId).stream()
                .map(EnrollmentService::toResponse)
                .toList();
    }

    // курсы студента
    public List<EnrollmentResponse> findByStudent(Long studentId) {
        return enrollmentRepository.findWithCourseByStudentId(studentId).stream()
                .map(EnrollmentService::toResponse)
                .toList();
    }

    private Enrollment getEnrollment(Long id) {
        return enrollmentRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Запись на курс с id=" + id + " не найдена"));
    }

    public static EnrollmentResponse toResponse(Enrollment enrollment) {
        return new EnrollmentResponse(
                enrollment.getId(),
                enrollment.getStudent().getId(),
                enrollment.getStudent().getName(),
                enrollment.getCourse().getId(),
                enrollment.getCourse().getTitle(),
                enrollment.getStatus(),
                enrollment.getEnrolledAt());
    }
}
