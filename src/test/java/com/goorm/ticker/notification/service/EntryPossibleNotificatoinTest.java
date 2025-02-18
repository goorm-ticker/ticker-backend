package com.goorm.ticker.notification.service;

import com.goorm.ticker.notification.scheduler.WaitingListScheduler;
import com.goorm.ticker.notification.repository.NotificationRepository;
import com.goorm.ticker.waitlist.entity.Status;
import com.goorm.ticker.waitlist.entity.WaitList;
import com.goorm.ticker.waitlist.repository.WaitListRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EntryPossibleNotificationTest {

    @Mock
    private WaitListRepository waitListRepository;

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private FCMService fcmService;

    @Mock
    private NotificationService notificationService;

    private WaitingListScheduler waitingListScheduler;

    private WaitList firstWaitListUser;
    private WaitList secondWaitListUser;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        waitingListScheduler = new WaitingListScheduler(waitListRepository, notificationService);

        // 대기 순번 1 사용자
        firstWaitListUser = WaitList.builder()
                .waitingId(1L)
                .waitingNumber(1)
                .status(Status.WAITING)
                .build();

        // 대기 순번 2 사용자
        secondWaitListUser = WaitList.builder()
                .waitingId(2L)
                .waitingNumber(2)
                .status(Status.WAITING)
                .build();
    }

    @Test
    @DisplayName("대기열이 있는 경우 알림을 전송한다")
    void EntryPossibleNotification() {
        // Given - 특정 레스토랑에 대기열이 존재하는 경우
        Long restaurantId = 1L;

        lenient().when(waitListRepository.findAllDistinctRestaurantIds()).thenReturn(List.of(restaurantId));
        lenient().when(waitListRepository.findRestaurantWaitingList(restaurantId)).thenReturn(List.of(firstWaitListUser));

        doAnswer(invocation -> {
            Long id = invocation.getArgument(0);
            return null;
        }).when(notificationService).sendEntryPossibleNotification(anyLong());

        // When
        waitingListScheduler.checkAndSendEntryPossibleNotification();

        // Then - 알림이 정상적으로 전송되었는지 확인
        verify(notificationService, times(1)).sendEntryPossibleNotification(anyLong());
    }

    @Test
    @DisplayName("대기열이 없는 경우 알림을 전송하지 않는다")
    void NoWaitingList() {
        // Given - 대기열이 없는 경우
        lenient().when(waitListRepository.findAllDistinctRestaurantIds()).thenReturn(List.of());

        // When
        waitingListScheduler.checkAndSendEntryPossibleNotification();

        // Then - 알림 전송이 되지 않아야 함
        verify(notificationService, never()).sendEntryPossibleNotification(anyLong());
    }
}