package com.f1.quiket.global.error;

import com.f1.quiket.global.response.ErrorCode;
import lombok.Getter;

/**
 * 애플리케이션 커스텀 예외 클래스
 * ErrorCode와 함께 예외를 throw하여 일관된 에러 처리 구조 제공
 */
@Getter
public class CustomException extends RuntimeException {

    private final ErrorCode errorCode;

    public CustomException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public CustomException(ErrorCode errorCode, String customMessage) {
        super(customMessage);
        this.errorCode = errorCode;
    }

    public CustomException(ErrorCode errorCode, Throwable cause) {
        super(errorCode.getMessage(), cause);
        this.errorCode = errorCode;
    }

    public CustomException(ErrorCode errorCode, String customMessage, Throwable cause) {
        super(customMessage, cause);
        this.errorCode = errorCode;
    }
}
