package com.goorm.ticker.notification.service;

import com.goorm.ticker.fixture.ReservationFixture;
import com.goorm.ticker.fixture.RestaurantFixture;
import com.goorm.ticker.fixture.ReservationSlotFixture;
import com.goorm.ticker.notification.dto.NotificationRequest;
import com.goorm.ticker.notification.dto.NotificationResponse;
import com.goorm.ticker.notification.entity.Notification;
import com.goorm.ticker.notification.entity.NotificationType;
import com.goorm.ticker.notification.publisher.ReservationStatusPublisher;
import com.goorm.ticker.notification.repository.NotificationRepository;
import com.goorm.ticker.reservation.Entity.Reservation;
import com.goorm.ticker.reservation.repository.ReservationRepository;
import com.goorm.ticker.restaurant.entity.ReservationSlot;
import com.goorm.ticker.restaurant.entity.Restaurant;
import com.goorm.ticker.user.entity.User;
import com.goorm.ticker.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ActiveProfiles("test")
public class NotificationServiceTest {

    @InjectMocks
    private NotificationService notificationService;

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private ReservationStatusPublisher reservationStatusPublisher;

    @Mock
    private FCMService fcmService;

    private User testUser;
    private Reservation testReservation;
    private Restaurant restaurantInstant;
    private ReservationSlot reservationSlotInstant;
    private Notification testNotification;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        testUser = new User(1L, "testUser", "testUser", "password1234");

        restaurantInstant = RestaurantFixture.RESTAURANT_FIXTURE_1.createRestaurant();
        reservationSlotInstant = ReservationSlotFixture.SLOT_FIXTURE_1.createSlot(restaurantInstant);

        testReservation = ReservationFixture.RESERVATION_FIXTURE_1.createReservation(
                restaurantInstant, reservationSlotInstant, testUser
        );

        testReservation = spy(testReservation);
        when(testReservation.getReservationId()).thenReturn(1L);

        testNotification = Notification.createNotification(
                testUser, "예약이 확정되었습니다.", NotificationType.RESERVATION_CONFIRMATION
        );

        when(userRepository.findById(anyLong())).thenReturn(Optional.of(testUser));
        when(notificationRepository.save(any())).thenReturn(testNotification);
        when(notificationRepository.findByUserIdOrderByCreatedAtDesc(eq(testUser.getId())))
                .thenReturn(List.of(testNotification));

        when(reservationRepository.findById(eq(1L))).thenReturn(Optional.of(testReservation));

        doNothing().when(reservationStatusPublisher).publishReservationStatus(anyLong(), anyString());

        when(testReservation.isNotificationAlreadySent(anyString())).thenReturn(false);

        doNothing().when(fcmService).sentNotification(anyString(), anyString(), anyString());
    }


    @Test
    @DisplayName("알림을 생성한다.")
    void createNotificationTest() {
        NotificationRequest request = new NotificationRequest(
                testUser.getId(),
                testReservation.getReservationId(),
                "예약이 확정되었습니다.",
                NotificationType.RESERVATION_CONFIRMATION
        );

        notificationService.createNotification(request);

        verify(notificationRepository, times(1)).save(any(Notification.class));

        verify(reservationStatusPublisher, times(1))
                .publishReservationStatus(eq(testReservation.getReservationId()), eq("CONFIRMED"));
    }



    @Test
    @DisplayName("사용자의 알림 목록을 조회한다.")
    void getNotificationsTest() {
        NotificationRequest request = new NotificationRequest(
                testUser.getId(),
                testReservation.getReservationId(),
                "예약이 확정되었습니다.",
                NotificationType.RESERVATION_CONFIRMATION
        );

        notificationService.createNotification(request);

        List<NotificationResponse> notifications = notificationService.getNotifications(testUser.getId());

        assertEquals(1, notifications.size());
        assertEquals("예약이 확정되었습니다.", notifications.get(0).getMessage());

        verify(notificationRepository, times(1)).findByUserIdOrderByCreatedAtDesc(eq(testUser.getId()));
    }
}
