package com.example.eduplatform.repository;

import com.example.eduplatform.entity.Lesson;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface LessonRepository extends JpaRepository<Lesson, Long> {

    List<Lesson> findByModuleIdOrderByOrderIndexAsc(Long moduleId);

    @Query("select l from Lesson l join fetch l.module where l.id = :id")
    Optional<Lesson> findWithModuleById(@Param("id") Long id);
}
