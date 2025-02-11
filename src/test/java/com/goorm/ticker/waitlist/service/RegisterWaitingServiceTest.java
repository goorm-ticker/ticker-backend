package com.goorm.ticker.waitlist.service;

import com.goorm.ticker.common.exception.CustomException;
import com.goorm.ticker.common.exception.ErrorCode;
import com.goorm.ticker.restaurant.entity.Restaurant;
import com.goorm.ticker.restaurant.repository.RestaurantRepository;
import com.goorm.ticker.user.entity.User;
import com.goorm.ticker.user.repository.UserRepository;
import com.goorm.ticker.waitlist.dto.WaitListRequestDto;
import com.goorm.ticker.waitlist.dto.WaitListResponseDto;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RegisterWaitingServiceTest {

    @InjectMocks
    private RegisterWaitingService registerWaitingService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private WaitListRepository waitListRepository;

    @Mock
    private RestaurantRepository restaurantRepository;

    @Mock
    private HttpSession session;

    private User testUser;
    private Restaurant testRestaurant;
    private WaitListRequestDto testRequest;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .build();

        testRestaurant = Restaurant.builder()
                .restaurantId(1L)
                .build();

        testRequest = WaitListRequestDto.builder()
                .restaurantId(1L)
                .build();
    }

    @Test
    @DisplayName("대기 등록 성공")
    void registerWaiting_Success() {
        // given
        when(session.getAttribute("user")).thenReturn(testUser.getId());
        when(userRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));
        when(restaurantRepository.findById(testRestaurant.getRestaurantId())).thenReturn(Optional.of(testRestaurant));
        when(waitListRepository.existsByRestaurant_RestaurantIdAndUser_IdAndStatus(testRestaurant.getRestaurantId(), testUser.getId(), Status.WAITING))
                .thenReturn(false);
        when(waitListRepository.findMaxWaitingNumberByRestaurantId(testRestaurant.getRestaurantId())).thenReturn(10);

        WaitList waitList = WaitList.builder()
                .user(testUser)
                .restaurant(testRestaurant)
                .waitingNumber(11)
                .status(Status.WAITING)
                .build();

        when(waitListRepository.save(any(WaitList.class))).thenReturn(waitList);

        // when
        WaitListResponseDto response = registerWaitingService.registerWaiting(testRequest);

        // then
        assertThat(response).isNotNull();
        assertThat(response.waitingNumber()).isEqualTo(11);
        verify(waitListRepository, times(1)).save(any(WaitList.class));
    }

    @Test
    @DisplayName("세션이 없는 경우 - SESSION_EXPIRED 예외 발생")
    void registerWaiting_Fail_NoSession() {
        // given
        when(session.getAttribute("user")).thenReturn(null);

        // when & then
        assertThatThrownBy(() -> registerWaitingService.registerWaiting(testRequest))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.SESSION_EXPIRED);
    }

    @Test
    @DisplayName("존재하지 않는 사용자 - NOT_FOUND_USER 발생")
    void registerWaiting_Fail_NotFoundUser() {
        // given
        when(session.getAttribute("user")).thenReturn(testUser.getId());
        when(userRepository.findById(testUser.getId())).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> registerWaitingService.registerWaiting(testRequest))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.NOT_FOUND_USER);
    }

    @Test
    @DisplayName("존재하지 않는 식당 - RESTAURANT_NOT_FOUND 예외 발생")
    void registerWaiting_Fail_RestaurantNotFound() {
        // given
        when(session.getAttribute("user")).thenReturn(testUser.getId());
        when(userRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));
        when(restaurantRepository.findById(testRestaurant.getRestaurantId())).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> registerWaitingService.registerWaiting(testRequest))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.RESTAURANT_NOT_FOUND);
    }

    @Test
    @DisplayName("이미 동일한 식당에 대기 중인 경우 - WAITLIST_ALREADY_EXISTS 예외 발생")
    void registerWaiting_Fail_DuplicateWaiting() {
        // given
        when(session.getAttribute("user")).thenReturn(testUser.getId());
        when(userRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));
        when(restaurantRepository.findById(testRestaurant.getRestaurantId())).thenReturn(Optional.of(testRestaurant));
        when(waitListRepository.existsByRestaurant_RestaurantIdAndUser_IdAndStatus(testRestaurant.getRestaurantId(), testUser.getId(), Status.WAITING))
                .thenReturn(true);

        // when & then
        assertThatThrownBy(() -> registerWaitingService.registerWaiting(testRequest))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.WAITLIST_ALREADY_EXISTS);
    }
}