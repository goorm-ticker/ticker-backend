package com.goorm.ticker.notification.dto;

import com.goorm.ticker.notification.entity.NotificationType;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class NotificationResponse {
    private final Long id;
    private final Long userId;
    private final String message;
    private final NotificationType type;
    private final boolean isRead;
    private final LocalDateTime createdAt;

    public NotificationResponse(Long id, Long userId, String message, NotificationType type, boolean isRead, LocalDateTime createdAt) {
        this.id = id;
        this.userId = userId;
        this.message = message;
        this.type = type;
        this.isRead = isRead;
        this.createdAt = createdAt;
    }
}