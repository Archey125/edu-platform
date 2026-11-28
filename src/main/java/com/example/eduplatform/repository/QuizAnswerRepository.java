package com.example.eduplatform.repository;

import com.example.eduplatform.entity.QuizAnswer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface QuizAnswerRepository extends JpaRepository<QuizAnswer, Long> {

    List<QuizAnswer> findByQuestionId(Long questionId);

    List<QuizAnswer> findByQuizSubmissionId(Long quizSubmissionId);

    @Modifying
    @Query("delete from QuizAnswer a where a.quizSubmission.student.id = :studentId")
    void deleteByStudentId(@Param("studentId") Long studentId);
}
