package com.f1.quiket.global.error;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.f1.quiket.global.response.ApiResponse;
import com.f1.quiket.global.response.ErrorCode;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    private ServletWebRequest webRequest() {
        return new ServletWebRequest(new MockHttpServletRequest());
    }

    private ApiResponse<?> body(ResponseEntity<?> response) {
        assertThat(response.getBody()).isInstanceOf(ApiResponse.class);
        return (ApiResponse<?>) response.getBody();
    }

    @Test
    void handleExceptionInternal_preserves_file_size_exceeded_response() {
        // MaxUploadSizeExceededException은 RESH가 매핑하므로 개별 핸들러를 둘 수 없어
        // handleExceptionInternal에서 타입 분기로 전용 응답(FILE_SIZE_EXCEEDED, 413)을 보존한다.
        ResponseEntity<Object> response = handler.handleExceptionInternal(
                new MaxUploadSizeExceededException(50 * 1024 * 1024),
                null, new HttpHeaders(), HttpStatus.PAYLOAD_TOO_LARGE, webRequest()
        );

        assertThat(response.getStatusCode().value()).isEqualTo(ErrorCode.FILE_SIZE_EXCEEDED.getStatus());
        assertThat(body(response).isSuccess()).isFalse();
        assertThat(body(response).getCode()).isEqualTo(ErrorCode.FILE_SIZE_EXCEEDED.getCode());
        assertThat(body(response).getMessage()).isEqualTo(ErrorCode.FILE_SIZE_EXCEEDED.getMessage());
    }

    @Test
    void handleExceptionInternal_maps_message_not_readable_to_400_with_fixed_message() {
        // 날짜/enum 역직렬화 실패 등 잘못된 본문은 400 + 고정 메시지 (PR #133 동작을 RESH 경로에서 보존)
        HttpMessageNotReadableException ex = new HttpMessageNotReadableException(
                "JSON parse error: Cannot deserialize value of type `java.time.LocalDate` from String \"2026.06.30\"",
                new RuntimeException("date parse failed"),
                null
        );

        ResponseEntity<Object> response = handler.handleExceptionInternal(
                ex, null, new HttpHeaders(), HttpStatus.BAD_REQUEST, webRequest()
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(body(response).isSuccess()).isFalse();
        assertThat(body(response).getCode()).isEqualTo(ErrorCode.INVALID_INPUT_VALUE.getCode());
        assertThat(body(response).getMessage()).isEqualTo("요청 본문의 형식이 올바르지 않습니다.");
    }

    @Test
    void handleExceptionInternal_maps_status_to_matching_error_code() {
        assertThat(body(internal(HttpStatus.METHOD_NOT_ALLOWED)).getCode())
                .isEqualTo(ErrorCode.METHOD_NOT_ALLOWED.getCode());
        assertThat(body(internal(HttpStatus.NOT_FOUND)).getCode())
                .isEqualTo(ErrorCode.NOT_FOUND.getCode());
        assertThat(body(internal(HttpStatus.UNSUPPORTED_MEDIA_TYPE)).getCode())
                .isEqualTo(ErrorCode.UNSUPPORTED_MEDIA_TYPE.getCode());
        assertThat(body(internal(HttpStatus.NOT_ACCEPTABLE)).getCode())
                .isEqualTo(ErrorCode.NOT_ACCEPTABLE.getCode());
        // 매핑되지 않은 5xx는 내부 서버 오류로 통일
        assertThat(body(internal(HttpStatus.INTERNAL_SERVER_ERROR)).getCode())
                .isEqualTo(ErrorCode.INTERNAL_SERVER_ERROR.getCode());
        // 매핑되지 않은 4xx는 잘못된 입력으로 통일
        assertThat(body(internal(HttpStatus.PAYLOAD_TOO_LARGE)).getCode())
                .isEqualTo(ErrorCode.INVALID_INPUT_VALUE.getCode());
    }

    private ResponseEntity<Object> internal(HttpStatus status) {
        return handler.handleExceptionInternal(
                new RuntimeException("boom"), null, new HttpHeaders(), status, webRequest()
        );
    }

    @Test
    void handleConstraintViolationException_returns_400_with_field_message() {
        // @Validated path/query 파라미터 검증 실패 — 기존에는 500으로 샜던 케이스
        ConstraintViolation<?> violation = mock(ConstraintViolation.class);
        Path path = mock(Path.class);
        when(path.toString()).thenReturn("checkEmail.email");
        when(violation.getPropertyPath()).thenReturn(path);
        when(violation.getMessage()).thenReturn("올바른 이메일 형식이 아닙니다");

        ResponseEntity<ApiResponse<?>> response = handler.handleConstraintViolationException(
                new ConstraintViolationException(Set.of(violation))
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode()).isEqualTo(ErrorCode.INVALID_INPUT_VALUE.getCode());
        assertThat(response.getBody().getMessage()).isEqualTo("email: 올바른 이메일 형식이 아닙니다");
    }

    @Test
    void handleConstraintViolationException_falls_back_when_no_violation() {
        ResponseEntity<ApiResponse<?>> response = handler.handleConstraintViolationException(
                new ConstraintViolationException(Set.of())
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).isEqualTo("입력값이 올바르지 않습니다.");
    }

    @Test
    void handleMethodArgumentNotValid_exposes_field_and_message() {
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);
        when(ex.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldError()).thenReturn(new FieldError("request", "examDate", "시험 날짜는 필수입니다"));

        ResponseEntity<Object> response = handler.handleMethodArgumentNotValid(
                ex, new HttpHeaders(), HttpStatus.BAD_REQUEST, webRequest()
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(body(response).getCode()).isEqualTo(ErrorCode.INVALID_INPUT_VALUE.getCode());
        assertThat(body(response).getMessage()).isEqualTo("examDate: 시험 날짜는 필수입니다");
    }

    @Test
    void handleGlobalException_returns_500() {
        ResponseEntity<ApiResponse<?>> response = handler.handleGlobalException(new RuntimeException("unexpected"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode()).isEqualTo(ErrorCode.INTERNAL_SERVER_ERROR.getCode());
    }
}
