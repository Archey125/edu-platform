package com.example.eduplatform.service;

import com.example.eduplatform.dto.ReviewRequest;
import com.example.eduplatform.dto.ReviewResponse;
import com.example.eduplatform.entity.Course;
import com.example.eduplatform.entity.CourseReview;
import com.example.eduplatform.entity.User;
import com.example.eduplatform.entity.enums.UserRole;
import com.example.eduplatform.exception.BusinessRuleException;
import com.example.eduplatform.exception.DuplicateResourceException;
import com.example.eduplatform.exception.NotFoundException;
import com.example.eduplatform.repository.CourseRepository;
import com.example.eduplatform.repository.CourseReviewRepository;
import com.example.eduplatform.repository.EnrollmentRepository;
import com.example.eduplatform.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

// отзывы о курсах: оставить может только записанный студент, один отзыв на курс
@Service
@Transactional(readOnly = true)
public class ReviewService {

    private static final Logger log = LoggerFactory.getLogger(ReviewService.class);

    private final CourseReviewRepository reviewRepository;
    private final CourseRepository courseRepository;
    private final UserRepository userRepository;
    private final EnrollmentRepository enrollmentRepository;

    public ReviewService(CourseReviewRepository reviewRepository,
                         CourseRepository courseRepository,
                         UserRepository userRepository,
                         EnrollmentRepository enrollmentRepository) {
        this.reviewRepository = reviewRepository;
        this.courseRepository = courseRepository;
        this.userRepository = userRepository;
        this.enrollmentRepository = enrollmentRepository;
    }

    @Transactional
    public ReviewResponse addReview(Long courseId, ReviewRequest request) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new NotFoundException("Курс с id=" + courseId + " не найден"));
        User student = userRepository.findById(request.studentId())
                .orElseThrow(() -> new NotFoundException("Пользователь с id=" + request.studentId() + " не найден"));
        if (student.getRole() != UserRole.STUDENT) {
            throw new BusinessRuleException("Отзыв может оставить только студент");
        }
        if (!enrollmentRepository.existsByStudentIdAndCourseId(student.getId(), courseId)) {
            throw new BusinessRuleException("Отзыв можно оставить только на курс, на который вы записаны");
        }
        if (reviewRepository.existsByCourseIdAndStudentId(courseId, student.getId())) {
            throw new DuplicateResourceException("Отзыв студента '" + student.getName()
                    + "' на этот курс уже существует");
        }
        CourseReview review = new CourseReview();
        review.setCourse(course);
        review.setStudent(student);
        review.setRating(request.rating());
        review.setComment(request.comment());
        CourseReview saved = reviewRepository.save(review);
        log.info("Студент id={} оставил отзыв на курс id={} (оценка {}/5)", student.getId(), courseId, request.rating());
        return toResponse(saved);
    }

    public List<ReviewResponse> findByCourse(Long courseId) {
        return reviewRepository.findWithStudentByCourseId(courseId).stream()
                .map(ReviewService::toResponse)
                .toList();
    }

    public static ReviewResponse toResponse(CourseReview review) {
        return new ReviewResponse(review.getId(), review.getCourse().getId(), review.getStudent().getId(),
                review.getStudent().getName(), review.getRating(), review.getComment(), review.getCreatedAt());
    }
}
