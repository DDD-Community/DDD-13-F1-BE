package com.f1.quiket.global.error;

import com.f1.quiket.global.response.ApiResponse;
import com.f1.quiket.global.response.ErrorCode;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * 전역 예외 처리 핸들러
 *
 * <p>표준 Spring MVC 예외(본문 파싱 실패, 타입 불일치, 필수 파라미터 누락, 메서드 미지원,
 * 미디어타입 등)는 {@link ResponseEntityExceptionHandler}가 적절한 4xx/5xx로 분류하며,
 * 본 핸들러는 {@link #handleExceptionInternal}에서 응답 본문만 {@link ApiResponse} 포맷으로
 * 변환한다. 따라서 예외마다 개별 {@code @ExceptionHandler}를 나열하지 않아도 표준 예외가
 * 일관된 형식·상태코드로 응답된다.</p>
 *
 * <p>RESH가 다루지 않는 도메인 예외({@link CustomException}), Bean Validation 파라미터 검증
 * 실패({@link ConstraintViolationException}), 전용 응답이 필요한 케이스({@link
 * MaxUploadSizeExceededException}), 그리고 최종 fallback({@link Exception})만 개별 핸들러로
 * 둔다.</p>
 */
@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    /**
     * 커스텀 예외 처리
     */
    @ExceptionHandler(CustomException.class)
    public ResponseEntity<ApiResponse<?>> handleCustomException(CustomException e) {
        log.warn("Custom Exception: {} - {}", e.getErrorCode().getCode(), e.getMessage());
        return ResponseEntity
                .status(e.getErrorCode().getStatus())
                .body(ApiResponse.fail(e.getErrorCode(), e.getMessage(), e.getData()));
    }

    /**
     * {@code @Validated} 경로/쿼리 파라미터 검증 실패 (RESH 미커버).
     * 메서드 파라미터에 직접 붙은 제약(@Email, @NotBlank 등) 위반 시 발생하며,
     * 클라이언트 입력 오류이므로 400으로 응답한다.
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<?>> handleConstraintViolationException(ConstraintViolationException e) {
        String message = e.getConstraintViolations().stream()
                .findFirst()
                .map(this::formatViolation)
                .orElse("입력값이 올바르지 않습니다.");

        log.warn("Constraint violation: {}", message);
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.fail(ErrorCode.INVALID_INPUT_VALUE, message));
    }

    /**
     * 전역 fallback — 처리되지 않은 예외는 500
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<?>> handleGlobalException(Exception e) {
        log.error("Unhandled exception: ", e);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.fail(ErrorCode.INTERNAL_SERVER_ERROR));
    }

    /**
     * {@code @Valid} 본문 검증 실패 — 위반 필드/메시지를 노출하도록 RESH 기본 동작을 재정의.
     */
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request
    ) {
        String message = Optional.ofNullable(ex.getBindingResult().getFieldError())
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .orElse("입력값이 올바르지 않습니다.");

        log.warn("Validation failed: {}", message);
        return createResponseEntity(ApiResponse.fail(ErrorCode.INVALID_INPUT_VALUE, message), headers, status, request);
    }

    /**
     * RESH가 분류한 표준 MVC 예외의 공통 응답 변환.
     * 상태코드를 도메인 {@link ErrorCode}로 매핑하고 본문을 {@link ApiResponse}로 통일한다.
     * 내부 파싱/검증 상세는 응답에 노출하지 않고 원인만 로그로 남긴다.
     */
    @Override
    protected ResponseEntity<Object> handleExceptionInternal(
            Exception ex,
            Object body,
            HttpHeaders headers,
            HttpStatusCode statusCode,
            WebRequest request
    ) {
        // 파일 업로드 용량 초과는 전용 응답(FILE_SIZE_EXCEEDED, 413)을 보존한다.
        // RESH가 MaxUploadSizeExceededException을 이미 @ExceptionHandler로 매핑하므로
        // 개별 핸들러를 두면 매핑 충돌이 나, 여기서 타입 분기로 처리한다.
        if (ex instanceof MaxUploadSizeExceededException) {
            log.warn("Multipart upload size exceeded: {}", ex.getMessage());
            HttpStatusCode fileSizeStatus = HttpStatus.valueOf(ErrorCode.FILE_SIZE_EXCEEDED.getStatus());
            return createResponseEntity(ApiResponse.fail(ErrorCode.FILE_SIZE_EXCEEDED), headers, fileSizeStatus, request);
        }

        ErrorCode errorCode = resolveErrorCode(statusCode);
        String message = resolveMessage(ex, errorCode);

        if (statusCode.is5xxServerError()) {
            log.error("MVC exception ({}): ", statusCode, ex);
        } else {
            log.warn("MVC exception ({}): {}", statusCode, ex.getMessage());
        }
        return createResponseEntity(ApiResponse.fail(errorCode, message), headers, statusCode, request);
    }

    private String formatViolation(ConstraintViolation<?> violation) {
        String path = violation.getPropertyPath() == null ? "" : violation.getPropertyPath().toString();
        int lastDot = path.lastIndexOf('.');
        String field = lastDot >= 0 ? path.substring(lastDot + 1) : path;
        return field.isBlank() ? violation.getMessage() : field + ": " + violation.getMessage();
    }

    private ErrorCode resolveErrorCode(HttpStatusCode statusCode) {
        if (statusCode.equals(HttpStatus.METHOD_NOT_ALLOWED)) {
            return ErrorCode.METHOD_NOT_ALLOWED;
        }
        if (statusCode.equals(HttpStatus.NOT_FOUND)) {
            return ErrorCode.NOT_FOUND;
        }
        if (statusCode.equals(HttpStatus.UNSUPPORTED_MEDIA_TYPE)) {
            return ErrorCode.UNSUPPORTED_MEDIA_TYPE;
        }
        if (statusCode.equals(HttpStatus.NOT_ACCEPTABLE)) {
            return ErrorCode.NOT_ACCEPTABLE;
        }
        if (statusCode.equals(HttpStatus.SERVICE_UNAVAILABLE)) {
            return ErrorCode.SERVICE_UNAVAILABLE;
        }
        if (statusCode.is5xxServerError()) {
            return ErrorCode.INTERNAL_SERVER_ERROR;
        }
        // 그 외 4xx(본문 파싱 실패/타입 불일치/필수 파라미터 누락 등)는 잘못된 입력으로 통일
        return ErrorCode.INVALID_INPUT_VALUE;
    }

    private String resolveMessage(Exception ex, ErrorCode errorCode) {
        if (ex instanceof HttpMessageNotReadableException) {
            return "요청 본문의 형식이 올바르지 않습니다.";
        }
        return errorCode.getMessage();
    }
}
