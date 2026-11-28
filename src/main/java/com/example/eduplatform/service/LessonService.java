package com.example.eduplatform.service;

import com.example.eduplatform.dto.AssignmentRequest;
import com.example.eduplatform.dto.AssignmentResponse;
import com.example.eduplatform.dto.LessonRequest;
import com.example.eduplatform.dto.LessonResponse;
import com.example.eduplatform.entity.Assignment;
import com.example.eduplatform.entity.Lesson;
import com.example.eduplatform.exception.NotFoundException;
import com.example.eduplatform.repository.AssignmentRepository;
import com.example.eduplatform.repository.LessonRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

// уроки и задания внутри уроков
@Service
@Transactional(readOnly = true)
public class LessonService {

    private static final Logger log = LoggerFactory.getLogger(LessonService.class);

    private final LessonRepository lessonRepository;
    private final AssignmentRepository assignmentRepository;

    public LessonService(LessonRepository lessonRepository, AssignmentRepository assignmentRepository) {
        this.lessonRepository = lessonRepository;
        this.assignmentRepository = assignmentRepository;
    }

    public LessonResponse findById(Long id) {
        return toResponse(getLesson(id));
    }

    @Transactional
    public LessonResponse update(Long id, LessonRequest request) {
        Lesson lesson = getLesson(id);
        if (request.title() != null) {
            lesson.setTitle(request.title());
        }
        if (request.content() != null) {
            lesson.setContent(request.content());
        }
        if (request.videoUrl() != null) {
            lesson.setVideoUrl(request.videoUrl());
        }
        if (request.orderIndex() != null) {
            lesson.setOrderIndex(request.orderIndex());
        }
        log.info("Обновлён урок id={}", id);
        return toResponse(lesson);
    }

    // удаление урока каскадно удалит его задания и решения
    @Transactional
    public void delete(Long id) {
        Lesson lesson = getLesson(id);
        lessonRepository.delete(lesson);
        log.info("Удалён урок '{}' (id={}) вместе с заданиями и решениями", lesson.getTitle(), id);
    }

    @Transactional
    public AssignmentResponse addAssignment(Long lessonId, AssignmentRequest request) {
        Lesson lesson = getLesson(lessonId);
        Assignment assignment = new Assignment();
        assignment.setTitle(request.title());
        assignment.setDescription(request.description());
        assignment.setDueDate(request.dueDate());
        assignment.setMaxScore(request.maxScore() != null ? request.maxScore() : 100);
        lesson.addAssignment(assignment);
        assignmentRepository.save(assignment);
        log.info("К уроку id={} добавлено задание '{}' (макс. балл {})", lessonId, assignment.getTitle(), assignment.getMaxScore());
        return AssignmentService.toResponse(assignment);
    }

    private Lesson getLesson(Long id) {
        return lessonRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Урок с id=" + id + " не найден"));
    }

    public static LessonResponse toResponse(Lesson lesson) {
        List<AssignmentResponse> assignments = lesson.getAssignments().stream()
                .sorted(Comparator.comparing(Assignment::getId))
                .map(AssignmentService::toResponse)
                .toList();
        return new LessonResponse(lesson.getId(), lesson.getTitle(), lesson.getContent(),
                lesson.getVideoUrl(), lesson.getOrderIndex(), assignments);
    }
}
