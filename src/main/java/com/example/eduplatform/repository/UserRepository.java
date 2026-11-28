package com.example.eduplatform.repository;

import com.example.eduplatform.entity.User;
import com.example.eduplatform.entity.enums.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

// репозиторий юзеров, базовый crud из JpaRepository + свои запросы
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    List<User> findByRole(UserRole role);

    // юзер сразу с профилем, join fetch вместо ленивой загрузки
    @Query("select u from User u left join fetch u.profile where u.id = :id")
    Optional<User> findWithProfileById(@Param("id") Long id);
}
