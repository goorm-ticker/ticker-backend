package com.goorm.ticker.common.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // Users
    DUPLICATE_USERNAME(HttpStatus.BAD_REQUEST,"중복된 유저이름입니다."),
    NOT_FOUND_USER(HttpStatus.NOT_FOUND,"존재하지 않는 유저입니다."),
    LOGIN_FAIL(HttpStatus.UNAUTHORIZED,"비밀번호가 일치하지 않습니다.");

    private final HttpStatus status;
    private final String message;
}
