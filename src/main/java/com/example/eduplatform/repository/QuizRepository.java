package com.example.eduplatform.repository;

import com.example.eduplatform.entity.Quiz;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface QuizRepository extends JpaRepository<Quiz, Long> {

    Optional<Quiz> findByModuleId(Long moduleId);

    // тест целиком одним запросом: модуль, курс, вопросы и варианты
    @Query("""
            select distinct q from Quiz q
            join fetch q.module m
            join fetch m.course
            left join fetch q.questions qu
            left join fetch qu.options
            where q.id = :id
            """)
    Optional<Quiz> findWithQuestionsById(@Param("id") Long id);

    @Query("select q from Quiz q join fetch q.module where q.id = :id")
    Optional<Quiz> findWithModuleById(@Param("id") Long id);
}
