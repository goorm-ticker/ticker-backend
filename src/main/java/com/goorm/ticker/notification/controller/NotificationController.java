package com.goorm.ticker.notification.controller;

import com.goorm.ticker.notification.dto.NotificationRequest;
import com.goorm.ticker.notification.dto.NotificationResponse;
import com.goorm.ticker.notification.service.FCMService;
import com.goorm.ticker.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;
    private final FCMService fcmService;

    @PostMapping
    public ResponseEntity<Void> createNotification(@RequestBody NotificationRequest request) {
        notificationService.createNotification(request);
        return ResponseEntity.ok().build();
    }

    @GetMapping
    public ResponseEntity<List<NotificationResponse>> getUserNotifications(Principal principal) {
        Long userId = Long.parseLong(principal.getName());
        List<NotificationResponse> notifications = notificationService.getNotifications(userId);
        return ResponseEntity.ok(notifications);
    }

    @PatchMapping("/{notificationId}/read")
    public ResponseEntity<Void> markAsRead(@PathVariable Long notificationId) {
        notificationService.markAsRead(notificationId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/unread-count")
    public ResponseEntity<Long> countUnreadNotifications(Principal principal) {
        Long userId = Long.parseLong(principal.getName());
        long unreadCount = notificationService.countUnreadNotifications(userId);
        return ResponseEntity.ok(unreadCount);
    }

    @PostMapping("/send-fcm")
    public ResponseEntity<String> sendFCMNotification(@RequestParam String topic, @RequestParam String title, @RequestParam String body) {
        fcmService.sentNotification(topic, title, body);
        return ResponseEntity.ok("Notification sent successfully");
    }


}
