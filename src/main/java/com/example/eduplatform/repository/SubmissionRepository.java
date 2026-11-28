package com.example.eduplatform.repository;

import com.example.eduplatform.entity.Submission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

// репозиторий решений домашек
public interface SubmissionRepository extends JpaRepository<Submission, Long> {

    Optional<Submission> findByAssignmentIdAndStudentId(Long assignmentId, Long studentId);

    boolean existsByAssignmentIdAndStudentId(Long assignmentId, Long studentId);

    // решения по заданию, со студентом
    @Query("""
            select s from Submission s
            join fetch s.student
            where s.assignment.id = :assignmentId
            order by s.submittedAt desc
            """)
    List<Submission> findWithStudentByAssignmentId(@Param("assignmentId") Long assignmentId);

    // все решения студента, сразу с заданием и курсом чтобы можно было показать контекст
    @Query("""
            select s from Submission s
            join fetch s.assignment a
            join fetch a.lesson l
            join fetch l.module m
            join fetch m.course c
            where s.student.id = :studentId
            order by s.submittedAt desc
            """)
    List<Submission> findWithDetailsByStudentId(@Param("studentId") Long studentId);

    // решение со студентом и заданием, для выставления оценки и уведомления
    @Query("""
            select s from Submission s
            join fetch s.student
            join fetch s.assignment
            where s.id = :id
            """)
    Optional<Submission> findWithStudentAndAssignmentById(@Param("id") Long id);

    @Modifying
    @Query("delete from Submission s where s.student.id = :studentId")
    void deleteByStudentId(@Param("studentId") Long studentId);
}
