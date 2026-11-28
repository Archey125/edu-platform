package com.example.eduplatform.integration;

import com.example.eduplatform.entity.Course;
import com.example.eduplatform.entity.Enrollment;
import com.example.eduplatform.entity.User;
import com.example.eduplatform.exception.ConflictException;
import com.example.eduplatform.exception.DuplicateResourceException;
import com.example.eduplatform.exception.NotFoundException;
import com.example.eduplatform.repository.AssignmentRepository;
import com.example.eduplatform.repository.CourseRepository;
import com.example.eduplatform.repository.EnrollmentRepository;
import com.example.eduplatform.repository.LessonRepository;
import com.example.eduplatform.repository.ModuleRepository;
import com.example.eduplatform.service.CourseService;
import com.example.eduplatform.support.TestDataFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// crud курса и правила удаления: каскад контента, запрет удаления при наличии записей,
// m2m с тегами, фильтры. тесты не транзакционные: данные коммитятся и удаляются явно
@SpringBootTest
@ActiveProfiles("test")
class CourseCascadeTest {

    @Autowired private CourseService courseService;
    @Autowired private TestDataFactory factory;
    @Autowired private CourseRepository courseRepository;
    @Autowired private ModuleRepository moduleRepository;
    @Autowired private LessonRepository lessonRepository;
    @Autowired private AssignmentRepository assignmentRepository;
    @Autowired private EnrollmentRepository enrollmentRepository;

    @Test
    void updateCourseChangesFields() {
        User teacher = factory.teacher();
        Course course = factory.course(teacher, factory.category());

        var updated = courseService.update(course.getId(),
                new com.example.eduplatform.dto.UpdateCourseRequest(
                        "Новое название", "Новое описание", null, null, 20, null));

        assertThat(updated.title()).isEqualTo("Новое название");
        assertThat(updated.description()).isEqualTo("Новое описание");
        assertThat(updated.durationHours()).isEqualTo(20);
        // категория и преподаватель не изменились
        assertThat(updated.categoryId()).isEqualTo(course.getCategory().getId());
        assertThat(updated.teacherId()).isEqualTo(teacher.getId());
    }

    @Test
    void updateCourseWithNonTeacherRejected() {
        Course course = factory.course(factory.teacher(), factory.category());
        User student = factory.student();

        assertThatThrownBy(() -> courseService.update(course.getId(),
                new com.example.eduplatform.dto.UpdateCourseRequest(null, null, null, student.getId(), null, null)))
                .isInstanceOf(com.example.eduplatform.exception.BusinessRuleException.class)
                .hasMessageContaining("преподавателем");
    }

    @Test
    void deleteCourseCascadesAllContent() {
        User teacher = factory.teacher();
        Course course = factory.course(teacher, factory.category());
        var module = factory.module(course);
        var lesson = factory.lesson(module);
        var assignment = factory.assignment(lesson);

        assertThat(moduleRepository.findById(module.getId())).isPresent();
        assertThat(lessonRepository.findById(lesson.getId())).isPresent();
        assertThat(assignmentRepository.findById(assignment.getId())).isPresent();

        courseRepository.delete(course);

        // каскадное удаление контента: модуль -> урок -> задание
        assertThat(courseRepository.findById(course.getId())).isEmpty();
        assertThat(moduleRepository.findById(module.getId())).isEmpty();
        assertThat(lessonRepository.findById(lesson.getId())).isEmpty();
        assertThat(assignmentRepository.findById(assignment.getId())).isEmpty();
    }

    @Test
    void deleteCourseWithEnrollmentsIsForbidden() {
        User teacher = factory.teacher();
        Course course = factory.course(teacher, factory.category());
        Enrollment enrollment = factory.enroll(factory.student(), course);

        assertThatThrownBy(() -> courseService.delete(course.getId()))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("записи студентов");

        // курс и запись остались
        assertThat(courseRepository.findById(course.getId())).isPresent();
        assertThat(enrollmentRepository.findById(enrollment.getId())).isPresent();

        // после отписки курс можно удалить
        enrollmentRepository.delete(enrollment);
        courseService.delete(course.getId());
        assertThat(courseRepository.findById(course.getId())).isEmpty();
    }

    @Test
    void deleteUnknownCourseThrowsNotFound() {
        assertThatThrownBy(() -> courseService.delete(999_999_999L))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void addModuleToCourse() {
        Course course = factory.course(factory.teacher(), factory.category());

        var module = courseService.addModule(course.getId(),
                new com.example.eduplatform.dto.ModuleRequest("Новый модуль", "Описание", null));

        assertThat(module.id()).isNotNull();
        assertThat(module.orderIndex()).isEqualTo(1); // автоматически следующий по порядку
        var second = courseService.addModule(course.getId(),
                new com.example.eduplatform.dto.ModuleRequest("Второй модуль", null, 5));
        assertThat(second.orderIndex()).isEqualTo(5); // явный порядок
    }

    @Test
    void tagsAreAttachedAndDetachedViaJoinTable() {
        Course course = factory.course(factory.teacher(), factory.category());
        var tag = factory.tag();

        var withTag = courseService.attachTag(course.getId(), tag.getId());
        assertThat(withTag.tags()).containsExactly(tag.getName());

        // повторная привязка того же тега - конфликт
        assertThatThrownBy(() -> courseService.attachTag(course.getId(), tag.getId()))
                .isInstanceOf(DuplicateResourceException.class);

        var withoutTag = courseService.detachTag(course.getId(), tag.getId());
        assertThat(withoutTag.tags()).isEmpty();
    }

    @Test
    void coursesAreFilteredByCategoryAndTeacher() {
        User teacher = factory.teacher();
        var category = factory.category();
        Course course1 = factory.course(teacher, category);
        Course course2 = factory.course(teacher, category);
        Course course3 = factory.course(factory.teacher(), factory.category());

        var byCategory = courseService.findAll(category.getId(), null, null);
        assertThat(byCategory).extracting(com.example.eduplatform.dto.CourseSummaryResponse::id)
                .containsExactlyInAnyOrder(course1.getId(), course2.getId())
                .doesNotContain(course3.getId());

        var byTeacher = courseService.findAll(null, null, teacher.getId());
        assertThat(byTeacher).extracting(com.example.eduplatform.dto.CourseSummaryResponse::id)
                .containsExactlyInAnyOrder(course1.getId(), course2.getId());
    }

    @Test
    void courseDetailContainsFullStructure() {
        User teacher = factory.teacher();
        Course course = factory.course(teacher, factory.category());
        var module = factory.module(course);
        var lesson = factory.lesson(module);
        var assignment = factory.assignment(lesson);

        var detail = courseService.findDetailById(course.getId());
        assertThat(detail.modules()).hasSize(1);
        assertThat(detail.modules().get(0).lessons()).hasSize(1);
        assertThat(detail.modules().get(0).lessons().get(0).assignments())
                .extracting(com.example.eduplatform.dto.AssignmentResponse::title)
                .containsExactly(assignment.getTitle());
        assertThat(detail.teacherName()).isEqualTo(teacher.getName());
    }
}
