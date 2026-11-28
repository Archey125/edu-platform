package com.example.eduplatform.service;

import com.example.eduplatform.dto.LessonRequest;
import com.example.eduplatform.dto.LessonResponse;
import com.example.eduplatform.dto.ModuleRequest;
import com.example.eduplatform.dto.ModuleResponse;
import com.example.eduplatform.entity.Lesson;
import com.example.eduplatform.entity.Module;
import com.example.eduplatform.entity.Quiz;
import com.example.eduplatform.exception.ConflictException;
import com.example.eduplatform.exception.NotFoundException;
import com.example.eduplatform.repository.LessonRepository;
import com.example.eduplatform.repository.ModuleRepository;
import com.example.eduplatform.repository.QuizSubmissionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

// модули курса и уроки внутри них
@Service
@Transactional(readOnly = true)
public class ModuleService {

    private static final Logger log = LoggerFactory.getLogger(ModuleService.class);

    private final ModuleRepository moduleRepository;
    private final LessonRepository lessonRepository;
    private final QuizSubmissionRepository quizSubmissionRepository;

    public ModuleService(ModuleRepository moduleRepository,
                         LessonRepository lessonRepository,
                         QuizSubmissionRepository quizSubmissionRepository) {
        this.moduleRepository = moduleRepository;
        this.lessonRepository = lessonRepository;
        this.quizSubmissionRepository = quizSubmissionRepository;
    }

    public ModuleResponse findById(Long id) {
        return toResponse(getModule(id));
    }

    @Transactional
    public ModuleResponse update(Long id, ModuleRequest request) {
        Module module = getModule(id);
        if (request.title() != null) {
            module.setTitle(request.title());
        }
        if (request.description() != null) {
            module.setDescription(request.description());
        }
        if (request.orderIndex() != null) {
            module.setOrderIndex(request.orderIndex());
        }
        log.info("Обновлён модуль id={}", id);
        return toResponse(module);
    }

    // удаление модуля каскадно снесет уроки -> задания -> решения и тест -> вопросы -> варианты.
    // если по тесту уже есть результаты студентов, удалять нельзя (409)
    @Transactional
    public void delete(Long id) {
        Module module = getModule(id);
        Quiz quiz = module.getQuiz();
        if (quiz != null && quizSubmissionRepository.existsByQuizId(quiz.getId())) {
            throw new ConflictException("Нельзя удалить модуль '" + module.getTitle()
                    + "': по его тесту уже есть результаты студентов.");
        }
        moduleRepository.delete(module);
        log.info("Удалён модуль '{}' (id={}) вместе с уроками, заданиями и тестом", module.getTitle(), id);
    }

    @Transactional
    public LessonResponse addLesson(Long moduleId, LessonRequest request) {
        Module module = getModule(moduleId);
        Lesson lesson = new Lesson();
        lesson.setTitle(request.title());
        lesson.setContent(request.content());
        lesson.setVideoUrl(request.videoUrl());
        lesson.setOrderIndex(request.orderIndex() != null
                ? request.orderIndex()
                : module.getLessons().size() + 1);
        module.addLesson(lesson);
        lessonRepository.save(lesson);
        log.info("В модуль id={} добавлен урок '{}'", moduleId, lesson.getTitle());
        return LessonService.toResponse(lesson);
    }

    private Module getModule(Long id) {
        return moduleRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Модуль с id=" + id + " не найден"));
    }

    public static ModuleResponse toResponse(Module module) {
        Long quizId = module.getQuiz() == null ? null : module.getQuiz().getId();
        List<LessonResponse> lessons = module.getLessons().stream()
                .sorted(Comparator
                        .comparing(Lesson::getOrderIndex, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(Lesson::getId))
                .map(LessonService::toResponse)
                .toList();
        return new ModuleResponse(module.getId(), module.getTitle(), module.getDescription(),
                module.getOrderIndex(), quizId, lessons);
    }
}
