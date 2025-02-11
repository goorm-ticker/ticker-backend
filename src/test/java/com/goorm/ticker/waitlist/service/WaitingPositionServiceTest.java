package com.goorm.ticker.waitlist.service;

import com.goorm.ticker.common.exception.CustomException;
import com.goorm.ticker.common.exception.ErrorCode;
import com.goorm.ticker.restaurant.entity.Restaurant;
import com.goorm.ticker.waitlist.entity.Status;
import com.goorm.ticker.waitlist.entity.WaitList;
import com.goorm.ticker.waitlist.repository.WaitListRepository;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;


import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class WaitingPositionServiceTest {

    @InjectMocks
    private WaitingPositionService waitingPositionService;

    @Mock
    private WaitListRepository waitListRepository;

    @Mock
    private HttpSession session;

    @BeforeEach
    void setUp() {
        lenient().when(session.getAttribute("user")).thenReturn(1L);
    }

    @Test
    @DisplayName("대기 순번 조회 성공")
    void getUserWaitingPosition_Success() {
        // given
        Restaurant restaurant = Restaurant.builder().restaurantId(1L).build();

        WaitList waitList = WaitList.builder()
                .restaurant(restaurant)
                .waitingNumber(5)
                .status(Status.WAITING)
                .build();

        when(waitListRepository.findByRestaurant_RestaurantIdAndUser_IdAndStatus(1L, 1L, Status.WAITING))
                .thenReturn(Optional.of(waitList));

        when(waitListRepository.countByRestaurant_RestaurantIdAndWaitingNumberLessThanAndStatus(1L, 5, Status.WAITING))
                .thenReturn(1L);

        // when
        int position = waitingPositionService.getUserWaitingPosition(1L);

        // then
        assertThat(position).isEqualTo(1);
    }

    @Test
    @DisplayName("대기 순번 조회 실패 - 세션 없음 (SESSION_EXPIRED)")
    void getUserWaitingPosition_Fail_NoSession() {
        // given
        when(session.getAttribute("user")).thenReturn(null);

        // when & then
        assertThatThrownBy(() -> waitingPositionService.getUserWaitingPosition(1L))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.SESSION_EXPIRED);
    }

    @Test
    @DisplayName("대기 순번 조회 실패 - 대기열에 없는 사용자 (WAITINGLIST_NOT_FOUND)")
    void getUserWaitingPosition_Fail_NoWaitList() {
        // given
        when(waitListRepository.findByRestaurant_RestaurantIdAndUser_IdAndStatus(1L, 1L, Status.WAITING))
                .thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> waitingPositionService.getUserWaitingPosition(1L))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.WAITLIST_NOT_FOUND);
    }

    @Test
    @DisplayName("총 대기 인원 조회 성공")
    void getTotalWaitingPosition_Success() {
        // given
        when(waitListRepository.countTotalWaitingByRestaurantId(1L)).thenReturn(5L);

        // when
        long totalWaiting = waitingPositionService.getTotalWaitingCount(1L);

        // then
        assertThat(totalWaiting).isEqualTo(5L);
    }
}
