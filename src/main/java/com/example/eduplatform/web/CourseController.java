package com.example.eduplatform.web;

import com.example.eduplatform.dto.CourseDetailResponse;
import com.example.eduplatform.dto.CourseSummaryResponse;
import com.example.eduplatform.dto.CreateCourseRequest;
import com.example.eduplatform.dto.EnrollRequest;
import com.example.eduplatform.dto.EnrollmentResponse;
import com.example.eduplatform.dto.ModuleRequest;
import com.example.eduplatform.dto.ModuleResponse;
import com.example.eduplatform.dto.ReviewRequest;
import com.example.eduplatform.dto.ReviewResponse;
import com.example.eduplatform.dto.UpdateCourseRequest;
import com.example.eduplatform.service.CourseService;
import com.example.eduplatform.service.EnrollmentService;
import com.example.eduplatform.service.ReviewService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// rest api курсов /api/courses: создание, полная структура, фильтры, теги, студенты, отзывы
@RestController
@RequestMapping("/api/courses")
public class CourseController {

    private final CourseService courseService;
    private final EnrollmentService enrollmentService;
    private final ReviewService reviewService;

    public CourseController(CourseService courseService,
                            EnrollmentService enrollmentService,
                            ReviewService reviewService) {
        this.courseService = courseService;
        this.enrollmentService = enrollmentService;
        this.reviewService = reviewService;
    }

    @PostMapping
    public ResponseEntity<CourseDetailResponse> create(@Valid @RequestBody CreateCourseRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(courseService.create(request));
    }

    @GetMapping
    public List<CourseSummaryResponse> findAll(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Long tagId,
            @RequestParam(required = false) Long teacherId) {
        return courseService.findAll(categoryId, tagId, teacherId);
    }

    @GetMapping("/{id}")
    public CourseDetailResponse findById(@PathVariable Long id) {
        return courseService.findDetailById(id);
    }

    @PutMapping("/{id}")
    public CourseDetailResponse update(@PathVariable Long id, @Valid @RequestBody UpdateCourseRequest request) {
        return courseService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        courseService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/modules")
    public ResponseEntity<ModuleResponse> addModule(@PathVariable Long id,
                                                    @Valid @RequestBody ModuleRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(courseService.addModule(id, request));
    }

    @PostMapping("/{id}/tags/{tagId}")
    public CourseDetailResponse attachTag(@PathVariable Long id, @PathVariable Long tagId) {
        return courseService.attachTag(id, tagId);
    }

    @DeleteMapping("/{id}/tags/{tagId}")
    public CourseDetailResponse detachTag(@PathVariable Long id, @PathVariable Long tagId) {
        return courseService.detachTag(id, tagId);
    }

    // запись на курс, тело {"userId": ...}
    @PostMapping("/{id}/enroll")
    public ResponseEntity<EnrollmentResponse> enroll(@PathVariable Long id,
                                                     @Valid @RequestBody EnrollRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(enrollmentService.enroll(id, request.userId()));
    }

    // студенты курса
    @GetMapping("/{id}/students")
    public List<EnrollmentResponse> getStudents(@PathVariable Long id) {
        return courseService.getEnrollments(id);
    }

    // отзывы курса
    @GetMapping("/{id}/reviews")
    public List<ReviewResponse> getReviews(@PathVariable Long id) {
        return reviewService.findByCourse(id);
    }

    @PostMapping("/{id}/reviews")
    @ResponseStatus(HttpStatus.CREATED)
    public ReviewResponse addReview(@PathVariable Long id, @Valid @RequestBody ReviewRequest request) {
        return reviewService.addReview(id, request);
    }
}
