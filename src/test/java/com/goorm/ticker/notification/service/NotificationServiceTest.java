package com.goorm.ticker.notification.service;

import com.goorm.ticker.notification.dto.NotificationRequest;
import com.goorm.ticker.notification.dto.NotificationResponse;
import com.goorm.ticker.notification.entity.Notification;
import com.goorm.ticker.notification.entity.NotificationType;
import com.goorm.ticker.notification.repository.NotificationRepository;
import com.goorm.ticker.user.entity.User;
import com.goorm.ticker.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
public class NotificationServiceTest {

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FCMService fcmService;

    private User testUser;

    @BeforeEach
    void setUp() {
        notificationRepository.deleteAll();
        userRepository.deleteAll();

        testUser = new User(null, "testUser", "testUser1", "password1234");
        userRepository.save(testUser);
    }

    @Test
    @DisplayName("알림을 생성한다.")
    void createNotificationTest() {
        NotificationRequest request = new NotificationRequest(testUser.getId(), "예약이 확정되었습니다.", NotificationType.RESERVATION_CONFIRMATION);

        notificationService.createNotification(request);

        List<Notification> notifications = notificationRepository.findByUserIdOrderByCreatedAtDesc(testUser.getId());
        assertEquals(1, notifications.size());
        assertEquals("예약이 확정되었습니다.", notifications.get(0).getMessage());
    }

    @Test
    @DisplayName("사용자의 알림 목록을 조회한다.")
    void getNotificationsTest() {
        NotificationRequest request1 = new NotificationRequest(testUser.getId(), "예약이 확정되었습니다.", NotificationType.RESERVATION_CONFIRMATION);
        NotificationRequest request2 = new NotificationRequest(testUser.getId(), "입장 가능합니다.", NotificationType.ENTRY_POSSIBLE);

        notificationService.createNotification(request1);
        notificationService.createNotification(request2);

        List<NotificationResponse> notifications = notificationService.getNotifications(testUser.getId());

        assertEquals(2, notifications.size());
        assertEquals("입장 가능합니다.", notifications.get(0).getMessage());
        assertEquals("예약이 확정되었습니다.", notifications.get(1).getMessage());
    }

    @Test
    @DisplayName("알림 읽음 처리한다.")
    void markAsReadTest() {
        Notification notification = Notification.createNotification(testUser, "테스트 메시지", NotificationType.RESERVATION_CONFIRMATION);
        notificationRepository.save(notification);

        notificationService.markAsRead(notification.getId());

        Notification updatedNotification = notificationRepository.findById(notification.getId()).orElseThrow();
        assertTrue(updatedNotification.isRead());
    }

    @Test
    @DisplayName("읽지 않은 알림 수를 조회한다.")
    void countUnreadNotificationsTest() {
        NotificationRequest request1 = new NotificationRequest(testUser.getId(), "예약 확정 알림", NotificationType.RESERVATION_CONFIRMATION);
        NotificationRequest request2 = new NotificationRequest(testUser.getId(), "입장 가능 알림", NotificationType.ENTRY_POSSIBLE);
        notificationService.createNotification(request1);
        notificationService.createNotification(request2);

        long unreadCount = notificationService.countUnreadNotifications(testUser.getId());

        assertEquals(2, unreadCount);
    }
}
