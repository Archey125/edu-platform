package com.example.eduplatform.repository;

import com.example.eduplatform.entity.Enrollment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

// записи на курс, промежуточная сущность m2m user <-> course
public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {

    Optional<Enrollment> findByStudentIdAndCourseId(Long studentId, Long courseId);

    boolean existsByStudentIdAndCourseId(Long studentId, Long courseId);

    boolean existsByCourseId(Long courseId);

    // кто записан на курс, join fetch чтобы имя студента было доступно вне транзакции
    @Query("""
            select e from Enrollment e
            join fetch e.student
            where e.course.id = :courseId
            order by e.enrolledAt desc
            """)
    List<Enrollment> findWithStudentByCourseId(@Param("courseId") Long courseId);

    // курсы студента
    @Query("""
            select e from Enrollment e
            join fetch e.course
            where e.student.id = :studentId
            order by e.enrolledAt desc
            """)
    List<Enrollment> findWithCourseByStudentId(@Param("studentId") Long studentId);

    @Modifying
    @Query("delete from Enrollment e where e.student.id = :studentId")
    void deleteByStudentId(@Param("studentId") Long studentId);
}
