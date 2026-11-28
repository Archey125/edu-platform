package com.example.eduplatform.repository;

import com.example.eduplatform.entity.Assignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AssignmentRepository extends JpaRepository<Assignment, Long> {

    List<Assignment> findByLessonId(Long lessonId);

    @Query("select a from Assignment a join fetch a.lesson where a.id = :id")
    Optional<Assignment> findWithLessonById(@Param("id") Long id);

    // задание сразу с уроком, модулем и курсом, чтобы проверить записан ли студент на курс
    @Query("""
            select a from Assignment a
            join fetch a.lesson l
            join fetch l.module m
            join fetch m.course c
            where a.id = :id
            """)
    Optional<Assignment> findWithCourseContextById(@Param("id") Long id);
}
