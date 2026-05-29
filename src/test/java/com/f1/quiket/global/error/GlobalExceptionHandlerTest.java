package com.f1.quiket.global.error;

import static org.assertj.core.api.Assertions.assertThat;

import com.f1.quiket.global.response.ApiResponse;
import com.f1.quiket.global.response.ErrorCode;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
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
}
