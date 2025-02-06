package com.goorm.ticker.notification.dto;

import com.goorm.ticker.notification.entity.NotificationType;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class NotificationRequest {
    private Long userId;
    private String message;
    private NotificationType type;

    public NotificationRequest(Long userId, String message, NotificationType type) {
        this.userId = userId;
        this.message = message;
        this.type = type;
    }
}