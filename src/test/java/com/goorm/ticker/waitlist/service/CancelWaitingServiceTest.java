package com.goorm.ticker.waitlist.service;

import com.goorm.ticker.common.exception.CustomException;
import com.goorm.ticker.common.exception.ErrorCode;
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

import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class CancelWaitingServiceTest {

    @InjectMocks
    private CancelWaitingService cancelWaitingService;

    @Mock
    private WaitListRepository waitListRepository;

    @Mock
    private HttpSession session;

    @BeforeEach
    void setUp() {
        when(session.getAttribute("user")).thenReturn(1L);
    }

    @Test
    @DisplayName("대기열 취소 성공")
    void cancelWaiting_Success() {
        // given
        WaitList waitList = WaitList.builder()
                .waitingNumber(1)
                .status(Status.WAITING)
                .build();

        when(waitListRepository.findByUser_IdAndStatus(1L, Status.WAITING))
                .thenReturn(Optional.of(waitList));

        // when
        cancelWaitingService.cancelWaiting();

        // when
        assert waitList.getStatus() == Status.CANCELLED;
    }

    @Test
    @DisplayName("대기열 취소 실패 - 세션 없음 (SESSION_EXPIRED)")
    void cancelWaiting_Fail_SessionExpired() {
        // given
        when(session.getAttribute("user")).thenReturn(null);

        // when & then
        assertThatThrownBy(() -> cancelWaitingService.cancelWaiting())
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.SESSION_EXPIRED);
    }

    @Test
    @DisplayName("대기열 취소 실패 - 대기열에 없는 사용자 (WAITLIST_NOT_FOUND)")
    void cancelWaiting_Fail_UserNotInWaitingList() {
        // given
        when(waitListRepository.findByUser_IdAndStatus(1L, Status.WAITING))
                .thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> cancelWaitingService.cancelWaiting())
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.WAITLIST_NOT_FOUND);
    }
}
