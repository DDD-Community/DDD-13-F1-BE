package com.f1.quiket.global.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 공통 API 응답 포맷
 * 상태 코드, 코드, 메시지, 데이터를 포함하여 일관된 응답 구조 제공
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    private int status;
    private String code;
    private String message;
    private T data;

    /**
     * 성공 응답 생성
     */
    public static <T> ApiResponse<T> of(SuccessCode successCode, T data) {
        return ApiResponse.<T>builder()
                .status(successCode.getStatus())
                .code(successCode.getCode())
                .message(successCode.getMessage())
                .data(data)
                .build();
    }

    /**
     * 성공 응답 생성 (데이터 없음)
     */
    public static <T> ApiResponse<T> of(SuccessCode successCode) {
        return ApiResponse.<T>builder()
                .status(successCode.getStatus())
                .code(successCode.getCode())
                .message(successCode.getMessage())
                .build();
    }

    /**
     * 실패 응답 생성
     */
    public static <T> ApiResponse<T> of(ErrorCode errorCode) {
        return ApiResponse.<T>builder()
                .status(errorCode.getStatus())
                .code(errorCode.getCode())
                .message(errorCode.getMessage())
                .build();
    }

    /**
     * 커스텀 메시지와 함께 실패 응답 생성
     */
    public static <T> ApiResponse<T> of(ErrorCode errorCode, String customMessage) {
        return ApiResponse.<T>builder()
                .status(errorCode.getStatus())
                .code(errorCode.getCode())
                .message(customMessage)
                .build();
    }
}
