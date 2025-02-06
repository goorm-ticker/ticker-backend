package com.goorm.ticker.notification.dto;

import com.goorm.ticker.notification.entity.NotificationType;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class NotificationResponse {
    private final Long id;
    private final Long userId;
    private final String message;
    private final NotificationType type;
    private final boolean isRead;
    private final LocalDateTime createdAt;
}