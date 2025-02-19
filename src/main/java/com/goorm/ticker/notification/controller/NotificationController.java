package com.goorm.ticker.notification.controller;

import com.goorm.ticker.notification.dto.NotificationResponse;
import com.goorm.ticker.notification.entity.NotificationType;
import com.goorm.ticker.notification.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name="Notification", description = "알림 API 제공")
@RestController
@RequestMapping("/notification")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @Operation(summary = "예약 확정/취소 알림 전송", description = "사용자에게 예약 확정/취소 알림을 전송한다.")
    @PostMapping("/reservation")
    public ResponseEntity<Void> sendReservationNotification(
            @RequestParam @NotNull Long reservationId,
            @RequestParam @NotNull NotificationType notificationType) {

        notificationService.sendReservationNotification(reservationId, notificationType);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "입장 가능 알림 전송", description = "사용자에게 입장 가능 알림을 전송한다.")
    @PostMapping("/entry-possible")
    public ResponseEntity<Void> sendEntryPossibleNotification(
            @RequestParam @NotNull Long restaurantId
    ) {
        notificationService.sendEntryPossibleNotification(restaurantId);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "사용자의 알림 목록 조회", description = "사용자의 알림 목록을 조회한다.")
    @GetMapping("/{userId}")
    public ResponseEntity<List<NotificationResponse>> getUserNotifications(
            @PathVariable Long userId
    ) {
        List<NotificationResponse> notifications = notificationService.getNotifications(userId);
        return ResponseEntity.ok(notifications);
    }

    @Operation(summary = "알림 읽음 처리", description = "특정 알림을 읽음 처리한다.")
    @PatchMapping("/{notificatoinId}/read")
    public ResponseEntity<Void> markNotificationAsRead(
            @PathVariable Long notificatoinId
    ) {
        notificationService.markAsRead(notificatoinId);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "읽지 않은 알림 개수 조회", description = "사용자의 읽지 않은 알림 개수를 조회한다.")
    @GetMapping("/{userId}/unread-count")
    public ResponseEntity<Long> countUnreadNotifications(
            @PathVariable Long userId
    ) {
        long unreadCount = notificationService.countUnreadNotifications(userId);
        return ResponseEntity.ok(unreadCount);
    }


}