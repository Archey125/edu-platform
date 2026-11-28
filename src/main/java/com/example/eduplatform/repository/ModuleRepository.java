package com.example.eduplatform.repository;

import com.example.eduplatform.entity.Module;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ModuleRepository extends JpaRepository<Module, Long> {

    List<Module> findByCourseIdOrderByOrderIndexAsc(Long courseId);

    @Query("select m from Module m join fetch m.course where m.id = :id")
    Optional<Module> findWithCourseById(@Param("id") Long id);
}
