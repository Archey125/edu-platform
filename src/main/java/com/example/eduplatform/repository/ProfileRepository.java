package com.example.eduplatform.repository;

import com.example.eduplatform.entity.Profile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ProfileRepository extends JpaRepository<Profile, Long> {

    Optional<Profile> findByUserId(Long userId);

    @Modifying
    @Query("delete from Profile p where p.user.id = :userId")
    void deleteByUserId(@Param("userId") Long userId);
}
