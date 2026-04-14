package com.f1.quiket.global.response;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 성공 응답 코드 enum
 * HTTP 2xx 상태와 매핑되는 성공 코드를 정의합니다.
 */
@Getter
@RequiredArgsConstructor
public enum SuccessCode {

    // 2xx Success
    OK(200, "S001", "요청이 성공했습니다."),
    CREATED(201, "S002", "리소스가 생성되었습니다."),
    ACCEPTED(202, "S003", "요청이 수락되었습니다."),
    NO_CONTENT(204, "S004", "요청이 성공했습니다. 반환 콘텐츠가 없습니다."),
    ;

    private final int status;
    private final String code;
    private final String message;
}
