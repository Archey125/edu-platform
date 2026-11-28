package com.example.eduplatform.repository;

import com.example.eduplatform.entity.Course;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

// репозиторий курсов
// тут же варианты жадной загрузки ленивых коллекций:
// join fetch в jpql и @EntityGraph, чтобы коллекции были доступны вне транзакции
public interface CourseRepository extends JpaRepository<Course, Long> {

    // курс с категорией, преподавателем, тегами и модулями, одна коллекция - безопасно
    @Query("""
            select distinct c from Course c
            left join fetch c.category
            left join fetch c.teacher
            left join fetch c.tags
            left join fetch c.modules
            where c.id = :id
            """)
    Optional<Course> findWithModulesById(@Param("id") Long id);

    // вся структура курса: модули -> уроки -> задания + тест модуля
    @Query("""
            select distinct c from Course c
            left join fetch c.category
            left join fetch c.teacher
            left join fetch c.tags
            left join fetch c.modules m
            left join fetch m.quiz
            left join fetch m.lessons l
            left join fetch l.assignments
            where c.id = :id
            """)
    Optional<Course> findWithFullStructureById(@Param("id") Long id);

    // курс с записями и студентами, через @EntityGraph вместо join fetch
    @EntityGraph(attributePaths = {"enrollments", "enrollments.student"})
    Optional<Course> findWithEnrollmentsById(@Param("id") Long id);

    List<Course> findByCategoryId(Long categoryId);

    List<Course> findByTeacherId(Long teacherId);

    boolean existsByTeacherId(Long teacherId);

    @Query("select distinct c from Course c join c.tags t where t.id = :tagId")
    List<Course> findByTagId(@Param("tagId") Long tagId);

    @Query("""
            select distinct c from Course c
            left join fetch c.category
            left join fetch c.teacher
            left join fetch c.tags
            """)
    List<Course> findAllWithDetails();

    boolean existsByCategoryId(Long categoryId);
}
