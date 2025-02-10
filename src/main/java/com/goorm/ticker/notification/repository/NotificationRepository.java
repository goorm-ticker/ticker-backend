package com.goorm.ticker.notification.repository;

import com.goorm.ticker.notification.entity.Notification;
import com.goorm.ticker.notification.entity.NotificationType;
import com.goorm.ticker.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification>findByUserIdOrderByCreatedAtDesc(Long userId);

    List<Notification> findByTypeAndMessage(NotificationType type, String message);

    long countByUserIdAndIsReadFalse(Long userId);

    boolean existsByUserAndType(User user, NotificationType type);
}
