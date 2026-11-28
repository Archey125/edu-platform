package com.example.eduplatform.service;

import com.example.eduplatform.dto.NotificationResponse;
import com.example.eduplatform.entity.Notification;
import com.example.eduplatform.entity.User;
import com.example.eduplatform.entity.enums.NotificationType;
import com.example.eduplatform.exception.NotFoundException;
import com.example.eduplatform.repository.NotificationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

// уведомления, создаются другими сервисами при событиях: запись на курс, решение, оценка, тест
@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final NotificationRepository notificationRepository;

    public NotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @Transactional
    public void notify(User recipient, NotificationType type, String message) {
        notificationRepository.save(new Notification(recipient, type, message));
        log.debug("Уведомление для '{}' ({}): {}", recipient.getName(), type, message);
    }

    public List<NotificationResponse> forUser(Long userId) {
        return notificationRepository.findByRecipientIdOrderByCreatedAtDesc(userId).stream()
                .map(NotificationService::toResponse)
                .toList();
    }

    @Transactional
    public void markRead(Long id) {
        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Уведомление с id=" + id + " не найдено"));
        notification.setRead(true);
    }

    public static NotificationResponse toResponse(Notification notification) {
        return new NotificationResponse(notification.getId(), notification.getRecipient().getId(),
                notification.getType(), notification.getMessage(), notification.getCreatedAt(),
                notification.isRead());
    }
}
