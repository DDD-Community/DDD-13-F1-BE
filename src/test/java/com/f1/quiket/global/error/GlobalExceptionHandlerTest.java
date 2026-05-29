package com.f1.quiket.global.error;

import static org.assertj.core.api.Assertions.assertThat;

import com.f1.quiket.global.response.ApiResponse;
import com.f1.quiket.global.response.ErrorCode;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleMaxUploadSizeExceededException_returns_file_size_exceeded_response() {
        ResponseEntity<ApiResponse<?>> response = handler.handleMaxUploadSizeExceededException(
                new MaxUploadSizeExceededException(50 * 1024 * 1024)
        );

        assertThat(response.getStatusCode().value()).isEqualTo(ErrorCode.FILE_SIZE_EXCEEDED.getStatus());
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isFalse();
        assertThat(response.getBody().getCode()).isEqualTo(ErrorCode.FILE_SIZE_EXCEEDED.getCode());
        assertThat(response.getBody().getMessage()).isEqualTo(ErrorCode.FILE_SIZE_EXCEEDED.getMessage());
    }

    @Test
    void handleHttpMessageNotReadableException_returns_400_invalid_input() {
        // 날짜/enum 역직렬화 실패 등 잘못된 요청 본문은 500이 아닌 400으로 응답해야 한다.
        HttpMessageNotReadableException exception = new HttpMessageNotReadableException(
                "JSON parse error: Cannot deserialize value of type `java.time.LocalDate` from String \"2026.06.30\"",
                new RuntimeException("date parse failed"),
                null
        );

        ResponseEntity<ApiResponse<?>> response = handler.handleHttpMessageNotReadableException(exception);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isFalse();
        assertThat(response.getBody().getCode()).isEqualTo(ErrorCode.INVALID_INPUT_VALUE.getCode());
        assertThat(response.getBody().getMessage()).isEqualTo("요청 본문의 형식이 올바르지 않습니다.");
    }
}
