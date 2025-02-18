package com.goorm.ticker.common.service;

import com.goorm.ticker.common.exception.CustomException;
import com.goorm.ticker.common.exception.ErrorCode;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SessionService {
    private final HttpSession httpSession;

    public Long getSessionUserId() {
        Long userId = (Long) httpSession.getAttribute("user");
        if (userId == null)
            throw new CustomException(ErrorCode.SESSION_EXPIRED);

        return userId;
    }
}
