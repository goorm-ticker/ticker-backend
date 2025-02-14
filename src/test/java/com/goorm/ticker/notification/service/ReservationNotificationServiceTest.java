package com.goorm.ticker.notification.service;

import com.goorm.ticker.notification.entity.NotificationType;
import com.goorm.ticker.notification.publisher.ReservationStatusPublisher;
import com.goorm.ticker.notification.repository.NotificationRepository;
import com.goorm.ticker.user.entity.User;
import com.goorm.ticker.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ReservationNotificationServiceTest {
    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private FCMService fcmService;

    @Mock
    private ReservationStatusPublisher reservationStatusPublisher;

    @InjectMocks
    private NotificationService notificationService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User(1L, "testUser", "testUser", "password1234");
    }

    @Test
    @DisplayName("예약 확정 알림 전송")
    void sendReservationConfirmNotification() {
        // given
        when(userRepository.findById(1L)).thenReturn(java.util.Optional.of(testUser));

        // when
        notificationService.sendReservationNotification(1L, NotificationType.RESERVATION_CONFIRMATION);

        // then
        verify(reservationStatusPublisher, times(1)).publishReservationStatus(1L, "CONFIRMED");
        verify(fcmService, times(1)).sentNotification("general", "예약 확정 알림", "예약이 확정되었습니다. 방문 시간에 맞춰 방문해주세요.");
    }

    @Test
    @DisplayName("예약 취소 알림 전송")
    void sendReservationCancelNotification() {
        // given
        when(userRepository.findById(1L)).thenReturn(java.util.Optional.of(testUser));

        // when
        notificationService.sendReservationNotification(1L, NotificationType.RESERVATION_CANCEL);

        // then
        verify(reservationStatusPublisher, times(1)).publishReservationStatus(1L, "CANCELLED");
        verify(fcmService, times(1)).sentNotification("general", "예약 취소 알림", "예약이 취소되었습니다.");
    }
}
