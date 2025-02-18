package com.goorm.ticker.waitlist.service;

import com.goorm.ticker.common.exception.CustomException;
import com.goorm.ticker.common.exception.ErrorCode;
import com.goorm.ticker.map.service.MapService;
import com.goorm.ticker.notification.service.NotificationService;
import com.goorm.ticker.restaurant.entity.Restaurant;
import com.goorm.ticker.user.entity.User;
import com.goorm.ticker.waitlist.entity.Status;
import com.goorm.ticker.waitlist.entity.WaitList;
import com.goorm.ticker.waitlist.repository.WaitListRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CompleteWaitingServiceTest {

    @InjectMocks
    private CompleteWaitingService completeWaitingService;

    @Mock
    private WaitListRepository waitListRepository;

    @Mock
    private MapService mapService;

    @Mock
    private NotificationService notificationService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .build();

        lenient().doNothing().when(notificationService).sendEntryPossibleNotification(anyLong());
    }

    @Test
    @DisplayName("입장 완료 성공")
    void completeWaiting_Success() {
        // given
        WaitList waitList = WaitList.builder()
                .user(testUser)
                .waitingNumber(1)
                .status(Status.WAITING)
                .restaurant(Restaurant.builder().restaurantId(1L).build())
                .build();

        when(waitListRepository.findByUser_IdAndStatus(1L, Status.WAITING))
                .thenReturn(Optional.of(waitList));

        // when
        completeWaitingService.completeWaiting(testUser.getId());

        // Then
        assertEquals(Status.ENTERED, waitList.getStatus());

        verify(notificationService, times(1)).sendEntryPossibleNotification(waitList.getRestaurant().getRestaurantId());

    }

    @Test
    @DisplayName("입장 완료 실패 - 대기열에 없는 사용자 (WAITLIST_NOT_FOUND)")
    void completeWaiting_Fail_UserNotInWaitingList() {
        // given
        when(waitListRepository.findByUser_IdAndStatus(1L, Status.WAITING))
                .thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> completeWaitingService.completeWaiting(testUser.getId()))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.WAITLIST_NOT_FOUND);
    }
}