package com.example.eduplatform.integration;

import com.example.eduplatform.entity.Course;
import com.example.eduplatform.entity.User;
import com.example.eduplatform.repository.CourseRepository;
import com.example.eduplatform.support.TestDataFactory;
import org.hibernate.Hibernate;
import org.hibernate.LazyInitializationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// демонстрация ленивой загрузки
// тесты НЕ транзакционные: каждый вызов репозитория идет в своей короткой транзакции,
// после чего сессия закрывается и сущность становится detached
@SpringBootTest
@ActiveProfiles("test")
class LazyLoadingTest {

    @Autowired private CourseRepository courseRepository;
    @Autowired private TestDataFactory factory;
    @Autowired private PlatformTransactionManager transactionManager;

    private Long courseId;
    private Long courseIdWithEnrollment;

    @BeforeEach
    void setUp() {
        User teacher = factory.teacher();
        Course course = factory.course(teacher, factory.category());
        factory.module(course);
        factory.module(course);
        courseId = course.getId();

        Course second = factory.course(factory.teacher(), factory.category());
        factory.enroll(factory.student(), second);
        courseIdWithEnrollment = second.getId();
    }

    @Test
    void lazyCollectionThrowsOutsideTransaction() {
        // грузим курс, findById открывает и сразу закрывает свою транзакцию
        Course course = courseRepository.findById(courseId).orElseThrow();
        // сессия уже закрыта, коллекция модулей не инициализирована
        assertThatThrownBy(() -> course.getModules().size())
                .as("Ленивая коллекция недоступна вне транзакции")
                .isInstanceOf(LazyInitializationException.class);
    }

    @Test
    void manyToOneIsLoadedAsLazyProxy() {
        Course course = courseRepository.findById(courseId).orElseThrow();
        // manyToOne lazy: преподаватель это прокси, реального select не было
        assertThat(Hibernate.isInitialized(course.getTeacher()))
                .as("Связь ManyToOne должна быть ленивой (прокси)")
                .isFalse();
        // попытка достучаться до прокси вне сессии дает ту же ошибку
        assertThatThrownBy(() -> course.getTeacher().getName())
                .isInstanceOf(LazyInitializationException.class);
    }

    @Test
    void joinFetchSolvesLazyProblem() {
        // join fetch в jpql: коллекции инициализированы и доступны даже вне транзакции
        Course course = courseRepository.findWithFullStructureById(courseId).orElseThrow();
        assertThat(course.getModules()).hasSize(2);
        course.getModules().forEach(module ->
                assertThat(Hibernate.isInitialized(module)).isTrue());
    }

    @Test
    void entityGraphSolvesLazyProblem() {
        Course course = courseRepository.findWithEnrollmentsById(courseIdWithEnrollment).orElseThrow();
        assertThat(course.getEnrollments()).hasSize(1);
        // вложенный путь enrollments.student тоже подгружен
        assertThat(Hibernate.isInitialized(
                course.getEnrollments().iterator().next().getStudent())).isTrue();
    }

    @Test
    void collectionAccessInsideTransactionWorks() {
        TransactionTemplate template = new TransactionTemplate(transactionManager);
        Integer moduleCount = template.execute(status -> {
            Course course = courseRepository.findById(courseId).orElseThrow();
            // внутри транзакции ленивая коллекция читается без проблем
            return course.getModules().size();
        });
        assertThat(moduleCount).isEqualTo(2);
    }
}
