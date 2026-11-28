package com.example.eduplatform.repository;

import com.example.eduplatform.entity.QuizSubmission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface QuizSubmissionRepository extends JpaRepository<QuizSubmission, Long> {

    boolean existsByQuizIdAndStudentId(Long quizId, Long studentId);

    boolean existsByQuizId(Long quizId);

    Optional<QuizSubmission> findByQuizIdAndStudentId(Long quizId, Long studentId);

    // результаты теста со студентами
    @Query("""
            select qs from QuizSubmission qs
            join fetch qs.student
            where qs.quiz.id = :quizId
            order by qs.score desc
            """)
    List<QuizSubmission> findWithStudentByQuizId(@Param("quizId") Long quizId);

    List<QuizSubmission> findByStudentId(Long studentId);

    @Modifying
    @Query("delete from QuizSubmission qs where qs.student.id = :studentId")
    void deleteByStudentId(@Param("studentId") Long studentId);
}
