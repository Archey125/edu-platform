package com.example.eduplatform.repository;

import com.example.eduplatform.entity.CourseReview;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CourseReviewRepository extends JpaRepository<CourseReview, Long> {

    boolean existsByCourseIdAndStudentId(Long courseId, Long studentId);

    // отзывы курса со студентами
    @Query("""
            select r from CourseReview r
            join fetch r.student
            where r.course.id = :courseId
            order by r.createdAt desc
            """)
    List<CourseReview> findWithStudentByCourseId(@Param("courseId") Long courseId);

    // bulk удаление отзывов студента, мимо persistence context
    @Modifying
    @Query("delete from CourseReview r where r.student.id = :studentId")
    void deleteByStudentId(@Param("studentId") Long studentId);
}
