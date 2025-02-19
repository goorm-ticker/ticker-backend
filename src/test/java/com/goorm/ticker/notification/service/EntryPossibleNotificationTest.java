package com.goorm.ticker.notification.service;

import static com.goorm.ticker.waitlist.entity.Status.WAITING;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.goorm.ticker.notification.publisher.ReservationStatusPublisher;
import com.goorm.ticker.notification.subscriber.ReservationStatusSubscriber;
import com.goorm.ticker.reservation.Entity.Reservation;
import com.goorm.ticker.reservation.Entity.ReservationStatus;
import com.goorm.ticker.reservation.repository.ReservationRepository;
import com.goorm.ticker.reservation.service.ReservationService;
import com.goorm.ticker.restaurant.entity.ReservationSlot;
import com.goorm.ticker.waitlist.entity.WaitList;
import com.goorm.ticker.waitlist.repository.WaitListRepository;
import com.goorm.ticker.map.service.MapService;
import com.goorm.ticker.user.entity.User;
import com.goorm.ticker.restaurant.entity.Restaurant;
import com.goorm.ticker.waitlist.service.CompleteWaitingService;
import com.goorm.ticker.restaurant.repository.RestaurantRepository;
import com.goorm.ticker.restaurant.repository.ReservationSlotRepository;
import com.goorm.ticker.user.repository.UserRepository;
import com.goorm.ticker.notification.service.FCMService;
import com.goorm.ticker.notification.repository.NotificationRepository;

import java.nio.charset.StandardCharsets;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.redis.connection.Message;
import org.springframework.test.context.ActiveProfiles;

@Slf4j
@ActiveProfiles("test")
class EntryPossibleNotificationTest {

    @Mock
    private WaitListRepository waitListRepository;

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private RestaurantRepository restaurantRepository;

    @Mock
    private ReservationSlotRepository reservationSlotRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private FCMService fcmService;

    @Mock
    private MapService mapService;

    @Mock
    private NotificationService notificationService;

    @Mock
    private ReservationStatusPublisher reservationStatusPublisher;

    @Mock
    private CompleteWaitingService completeWaitingService;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private ReservationService reservationService;
    private ReservationStatusSubscriber reservationStatusSubscriber;

    private WaitList testWaitList;
    private Reservation testReservation;
    private User testUser;
    private Restaurant testRestaurant;
    private ReservationSlot testReservationSlot;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        reservationService = new ReservationService(
                reservationRepository,
                restaurantRepository,
                reservationSlotRepository,
                userRepository,
                reservationStatusPublisher,
                fcmService,
                notificationRepository,
                completeWaitingService
        );

        reservationStatusSubscriber = new ReservationStatusSubscriber(
                notificationService,
                objectMapper,
                reservationService,
                reservationRepository,
                completeWaitingService
        );

        testRestaurant = Restaurant.builder()
                .restaurantId(1L)
                .restaurantName("Test Restaurant")
                .build();

        testUser = User.builder()
                .id(1L)
                .name("Test User")
                .build();

        testReservationSlot = ReservationSlot.builder()
                .id(1L)
                .slotTime(LocalTime.of(18, 30))
                .restaurant(testRestaurant)
                .availablePartySize(5)
                .build();

        testWaitList = WaitList.builder()
                .user(testUser)
                .restaurant(testRestaurant)
                .status(WAITING)
                .waitingNumber(1)
                .build();

        testReservation = Reservation.builder()
                .reservationId(1L)
                .user(testUser)
                .restaurant(testRestaurant)
                .reservationSlot(testReservationSlot)
                .status(ReservationStatus.CONFIRMED)
                .build();

        when(reservationRepository.findById(1L))
                .thenReturn(Optional.of(testReservation));


        when(waitListRepository.findByUser_IdAndStatus(1L, WAITING))
                .thenReturn(Optional.of(testWaitList));

        doAnswer(invocation -> {
            Long userId = invocation.getArgument(0);
            log.info("completeWaitingService 실행됨 - userId: {}", userId);
            return null;
        }).when(completeWaitingService).completeWaiting(anyLong());

        doNothing().when(notificationService).sendEntryPossibleNotification(anyLong());
    }

    @Test
    @DisplayName("입장 시 대기열이 감소하고 예약 상태가 변경되며 알림을 전송한다.")
    void EntryPossibleTest() throws Exception {
        when(reservationRepository.findById(1L))
                .thenReturn(Optional.of(testReservation));

        reservationService.updateReservation(1L, "ENTERED", 1L);

        verify(completeWaitingService, times(1)).completeWaiting(1L);

        String jsonMessage = "{\"reservationId\": 1, \"status\": \"ENTERED\"}";
        Message message = mock(Message.class);
        when(message.getBody()).thenReturn(jsonMessage.getBytes(StandardCharsets.UTF_8));

       try {
            String json = new String(message.getBody(), StandardCharsets.UTF_8);
            objectMapper.readTree(json);
        } catch (Exception e) {
            throw e;
        }

        reservationStatusSubscriber.onMessage(message, null);

        verify(completeWaitingService, atLeastOnce()).completeWaiting(1L);
        verify(notificationService, times(1)).sendEntryPossibleNotification(testWaitList.getRestaurant().getRestaurantId());
    }
}