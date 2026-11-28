package com.example.eduplatform.repository;

import com.example.eduplatform.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByRecipientIdOrderByCreatedAtDesc(Long userId);

    @Modifying
    @Query("delete from Notification n where n.recipient.id = :userId")
    void deleteByRecipientId(@Param("userId") Long userId);
}
