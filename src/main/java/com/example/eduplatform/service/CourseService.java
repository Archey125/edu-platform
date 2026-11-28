package com.example.eduplatform.service;

import com.example.eduplatform.dto.CourseDetailResponse;
import com.example.eduplatform.dto.CourseSummaryResponse;
import com.example.eduplatform.dto.CreateCourseRequest;
import com.example.eduplatform.dto.EnrollmentResponse;
import com.example.eduplatform.dto.ModuleRequest;
import com.example.eduplatform.dto.ModuleResponse;
import com.example.eduplatform.dto.UpdateCourseRequest;
import com.example.eduplatform.entity.Category;
import com.example.eduplatform.entity.Course;
import com.example.eduplatform.entity.Module;
import com.example.eduplatform.entity.Tag;
import com.example.eduplatform.entity.User;
import com.example.eduplatform.entity.enums.UserRole;
import com.example.eduplatform.exception.BusinessRuleException;
import com.example.eduplatform.exception.ConflictException;
import com.example.eduplatform.exception.DuplicateResourceException;
import com.example.eduplatform.exception.NotFoundException;
import com.example.eduplatform.repository.CategoryRepository;
import com.example.eduplatform.repository.CourseRepository;
import com.example.eduplatform.repository.EnrollmentRepository;
import com.example.eduplatform.repository.ModuleRepository;
import com.example.eduplatform.repository.TagRepository;
import com.example.eduplatform.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

// курсы и их содержимое: создание, структура модули/уроки/задания, теги, правила удаления
@Service
@Transactional(readOnly = true)
public class CourseService {

    private static final Logger log = LoggerFactory.getLogger(CourseService.class);

    private final CourseRepository courseRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final TagRepository tagRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final ModuleRepository moduleRepository;

    public CourseService(CourseRepository courseRepository,
                         CategoryRepository categoryRepository,
                         UserRepository userRepository,
                         TagRepository tagRepository,
                         EnrollmentRepository enrollmentRepository,
                         ModuleRepository moduleRepository) {
        this.courseRepository = courseRepository;
        this.categoryRepository = categoryRepository;
        this.userRepository = userRepository;
        this.tagRepository = tagRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.moduleRepository = moduleRepository;
    }

    // список курсов с фильтрами по категории / тегу / преподавателю
    public List<CourseSummaryResponse> findAll(Long categoryId, Long tagId, Long teacherId) {
        List<Course> courses;
        if (categoryId != null) {
            courses = courseRepository.findByCategoryId(categoryId);
        } else if (tagId != null) {
            courses = courseRepository.findByTagId(tagId);
        } else if (teacherId != null) {
            courses = courseRepository.findByTeacherId(teacherId);
        } else {
            courses = courseRepository.findAllWithDetails();
        }
        return courses.stream().map(CourseService::toSummary).toList();
    }

    // курс целиком, коллекции подтянуты join fetch
    public CourseDetailResponse findDetailById(Long id) {
        Course course = courseRepository.findWithFullStructureById(id)
                .orElseThrow(() -> new NotFoundException("Курс с id=" + id + " не найден"));
        return toDetail(course);
    }

    @Transactional
    public CourseDetailResponse create(CreateCourseRequest request) {
        Category category = categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> new NotFoundException("Категория с id=" + request.categoryId() + " не найдена"));
        User teacher = userRepository.findById(request.teacherId())
                .orElseThrow(() -> new NotFoundException("Пользователь с id=" + request.teacherId() + " не найден"));
        if (teacher.getRole() != UserRole.TEACHER) {
            throw new BusinessRuleException("Пользователь '" + teacher.getName()
                    + "' не является преподавателем: вести курс может только пользователь с ролью TEACHER");
        }
        Course course = new Course();
        course.setTitle(request.title());
        course.setDescription(request.description());
        course.setCategory(category);
        course.setTeacher(teacher);
        course.setDurationHours(request.durationHours());
        course.setStartDate(request.startDate());
        Course saved = courseRepository.save(course);
        log.info("Создан курс '{}' (id={}): преподаватель '{}', категория '{}'",
                saved.getTitle(), saved.getId(), teacher.getName(), category.getName());
        return toDetail(saved);
    }

    @Transactional
    public CourseDetailResponse update(Long id, UpdateCourseRequest request) {
        Course course = getCourse(id);
        if (request.title() != null) {
            course.setTitle(request.title());
        }
        if (request.description() != null) {
            course.setDescription(request.description());
        }
        if (request.categoryId() != null) {
            Category category = categoryRepository.findById(request.categoryId())
                    .orElseThrow(() -> new NotFoundException("Категория с id=" + request.categoryId() + " не найдена"));
            course.setCategory(category);
        }
        if (request.teacherId() != null) {
            User teacher = userRepository.findById(request.teacherId())
                    .orElseThrow(() -> new NotFoundException("Пользователь с id=" + request.teacherId() + " не найден"));
            if (teacher.getRole() != UserRole.TEACHER) {
                throw new BusinessRuleException("Пользователь '" + teacher.getName() + "' не является преподавателем");
            }
            course.setTeacher(teacher);
        }
        if (request.durationHours() != null) {
            course.setDurationHours(request.durationHours());
        }
        if (request.startDate() != null) {
            course.setStartDate(request.startDate());
        }
        log.info("Обновлён курс id={}", id);
        return findDetailById(id);
    }

    // удалить курс с записанными студентами нельзя (409), сохраняем историю обучения.
    // контент (модули/уроки/задания/тесты) удаляется каскадно вместе с курсом
    @Transactional
    public void delete(Long id) {
        if (enrollmentRepository.existsByCourseId(id)) {
            throw new ConflictException("Нельзя удалить курс id=" + id
                    + ": на него есть записи студентов. Сначала отзовите записи (удалите enrollments).");
        }
        Course course = getCourse(id);
        courseRepository.delete(course);
        log.info("Удалён курс '{}' (id={}) вместе с контентом: модули, уроки, задания, тесты", course.getTitle(), id);
    }

    // содержимое курса

    @Transactional
    public ModuleResponse addModule(Long courseId, ModuleRequest request) {
        Course course = getCourse(courseId);
        Module module = new Module();
        module.setTitle(request.title());
        module.setDescription(request.description());
        module.setOrderIndex(request.orderIndex() != null
                ? request.orderIndex()
                : course.getModules().size() + 1);
        course.addModule(module);
        moduleRepository.save(module);
        log.info("В курс id={} добавлен модуль '{}' (порядок {})", courseId, module.getTitle(), module.getOrderIndex());
        return ModuleService.toResponse(module);
    }

    // привязка тега, пример обычного m2m через join таблицу
    @Transactional
    public CourseDetailResponse attachTag(Long courseId, Long tagId) {
        Course course = getCourse(courseId);
        Tag tag = tagRepository.findById(tagId)
                .orElseThrow(() -> new NotFoundException("Тег с id=" + tagId + " не найден"));
        if (course.getTags().contains(tag)) {
            throw new DuplicateResourceException("Тег '" + tag.getName() + "' уже привязан к курсу");
        }
        course.addTag(tag);
        log.info("К курсу id={} привязан тег '{}'", courseId, tag.getName());
        return toDetail(course);
    }

    @Transactional
    public CourseDetailResponse detachTag(Long courseId, Long tagId) {
        Course course = getCourse(courseId);
        Tag tag = tagRepository.findById(tagId)
                .orElseThrow(() -> new NotFoundException("Тег с id=" + tagId + " не найден"));
        if (!course.getTags().contains(tag)) {
            throw new NotFoundException("Тег '" + tag.getName() + "' не привязан к курсу id=" + courseId);
        }
        course.removeTag(tag);
        log.info("С курса id={} снят тег '{}'", courseId, tag.getName());
        return toDetail(course);
    }

    // студенты курса
    public List<EnrollmentResponse> getEnrollments(Long courseId) {
        getCourse(courseId);
        return enrollmentRepository.findWithStudentByCourseId(courseId).stream()
                .map(EnrollmentService::toResponse)
                .toList();
    }

    private Course getCourse(Long id) {
        return courseRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Курс с id=" + id + " не найден"));
    }

    // маппинг в dto

    public static CourseSummaryResponse toSummary(Course course) {
        return new CourseSummaryResponse(
                course.getId(), course.getTitle(), course.getDescription(),
                course.getCategory().getId(), course.getCategory().getName(),
                course.getTeacher().getId(), course.getTeacher().getName(),
                course.getDurationHours(), course.getStartDate(), course.getCreatedAt(),
                course.getTags().stream().map(Tag::getName).sorted().toList());
    }

    public static CourseDetailResponse toDetail(Course course) {
        List<ModuleResponse> modules = course.getModules().stream()
                .sorted(Comparator
                        .comparing(Module::getOrderIndex, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(Module::getId))
                .map(ModuleService::toResponse)
                .toList();
        return new CourseDetailResponse(
                course.getId(), course.getTitle(), course.getDescription(),
                course.getCategory().getId(), course.getCategory().getName(),
                course.getTeacher().getId(), course.getTeacher().getName(),
                course.getDurationHours(), course.getStartDate(), course.getCreatedAt(),
                course.getTags().stream().map(Tag::getName).sorted().toList(),
                modules);
    }
}
