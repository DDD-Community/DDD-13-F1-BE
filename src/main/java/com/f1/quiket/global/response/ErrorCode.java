package com.f1.quiket.global.response;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 에러 응답 코드 enum
 * HTTP 4xx, 5xx 상태와 매핑되는 에러 코드를 정의합니다.
 */
@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // 4xx Client Errors
    INVALID_INPUT_VALUE(400, "C001", "잘못된 입력값입니다."),
    UNAUTHORIZED(401, "C005", "인증이 필요합니다."),
    FORBIDDEN(403, "C006", "접근 권한이 없습니다."),
    NOT_FOUND(404, "C003", "요청한 리소스를 찾을 수 없습니다."),
    METHOD_NOT_ALLOWED(405, "C004", "허용되지 않은 HTTP 메서드입니다."),
    CONFLICT(409, "C007", "이미 존재하는 리소스입니다."),
    UNPROCESSABLE_ENTITY(422, "C008", "처리할 수 없는 엔티티입니다."),

    // 5xx Server Errors
    INTERNAL_SERVER_ERROR(500, "C002", "서버 내부 오류가 발생했습니다."),
    SERVICE_UNAVAILABLE(503, "C009", "일시적으로 서비스를 이용할 수 없습니다."),
    ;

    private final int status;
    private final String code;
    private final String message;
}
