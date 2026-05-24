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

    // Auth Errors
    AUTH_EMAIL_ALREADY_EXISTS(409, "AUTH_EMAIL_ALREADY_EXISTS", "이미 사용 중인 이메일입니다."),
    AUTH_PASSWORD_CONFIRM_MISMATCH(400, "AUTH_PASSWORD_CONFIRM_MISMATCH", "비밀번호 확인이 일치하지 않습니다."),
    AUTH_USER_NOT_FOUND(404, "AUTH_USER_NOT_FOUND", "가입되지 않은 이메일입니다."),
    AUTH_INVALID_EMAIL_VERIFICATION(400, "AUTH_INVALID_EMAIL_VERIFICATION", "인증 코드 또는 토큰이 올바르지 않습니다."),
    AUTH_EMAIL_VERIFICATION_EXPIRED(400, "AUTH_EMAIL_VERIFICATION_EXPIRED", "이메일 인증 요청이 만료되었습니다."),
    AUTH_INVALID_CREDENTIALS(401, "AUTH_INVALID_CREDENTIALS", "이메일 또는 비밀번호가 올바르지 않습니다."),
    AUTH_EMAIL_NOT_VERIFIED(403, "AUTH_EMAIL_NOT_VERIFIED", "이메일 인증이 필요합니다."),
    AUTH_ACCOUNT_LOCKED(403, "AUTH_ACCOUNT_LOCKED", "계정이 잠금 처리되었습니다."),
    AUTH_LOCAL_IDENTITY_NOT_FOUND(409, "AUTH_LOCAL_IDENTITY_NOT_FOUND", "자체 로그인 인증 수단이 없는 계정입니다."),
    AUTH_PASSWORD_RESET_INVALID(400, "AUTH_PASSWORD_RESET_INVALID", "인증 코드 또는 토큰이 올바르지 않습니다."),
    AUTH_PASSWORD_RESET_EXPIRED(400, "AUTH_PASSWORD_RESET_EXPIRED", "비밀번호 재설정 요청이 만료되었습니다."),
    AUTH_PASSWORD_SAME_AS_PREVIOUS(400, "AUTH_PASSWORD_SAME_AS_PREVIOUS", "이전 비밀번호와 동일한 비밀번호는 사용할 수 없습니다."),
    AUTH_INVALID_TOKEN(401, "AUTH_INVALID_TOKEN", "토큰이 올바르지 않습니다."),
    AUTH_EXPIRED_TOKEN(401, "AUTH_EXPIRED_TOKEN", "토큰이 만료되었습니다."),
    AUTH_REVOKED_REFRESH_TOKEN(401, "AUTH_REVOKED_REFRESH_TOKEN", "폐기된 Refresh Token입니다."),
    AUTH_OAUTH_INVALID_TOKEN(401, "AUTH_OAUTH_INVALID_TOKEN", "Kakao Access Token이 올바르지 않습니다."),
    AUTH_OAUTH_EMAIL_REQUIRED(400, "AUTH_OAUTH_EMAIL_REQUIRED", "Kakao 계정의 유효한 이메일 제공 동의가 필요합니다."),
    AUTH_OAUTH_ACCOUNT_LINK_REQUIRED(409, "AUTH_OAUTH_ACCOUNT_LINK_REQUIRED", "동일 이메일로 가입된 계정이 있습니다. 계정 연동이 필요합니다."),
    AUTH_OAUTH_LINK_TOKEN_INVALID(401, "AUTH_OAUTH_LINK_TOKEN_INVALID", "Kakao 계정 연동 토큰이 올바르지 않거나 만료되었습니다."),
    AUTH_OAUTH_SIGNUP_TOKEN_INVALID(401, "AUTH_OAUTH_SIGNUP_TOKEN_INVALID", "Kakao 회원가입 토큰이 올바르지 않거나 만료되었습니다."),
    AUTH_OAUTH_ACCOUNT_ALREADY_LINKED(409, "AUTH_OAUTH_ACCOUNT_ALREADY_LINKED", "이미 Kakao 계정이 연동된 사용자입니다."),
    AUTH_OAUTH_PROVIDER_ALREADY_LINKED(409, "AUTH_OAUTH_PROVIDER_ALREADY_LINKED", "이미 다른 사용자에게 연동된 Kakao 계정입니다."),
    AUTH_OAUTH_TEMP_TOKEN_STORE_FAILED(503, "AUTH_OAUTH_TEMP_TOKEN_STORE_FAILED", "Kakao OAuth 임시 토큰 처리에 실패했습니다."),
    AUTH_OAUTH_USER_INFO_FAILED(503, "AUTH_OAUTH_USER_INFO_FAILED", "Kakao 사용자 정보 조회에 실패했습니다."),

    // Subject Errors
    SUBJECT_NOT_FOUND(404, "SUBJECT_NOT_FOUND", "과목을 찾을 수 없습니다."),

    // Quiz Errors
    QUIZ_OPTION_INVALID(400, "QUIZ_OPTION_INVALID", "퀴즈 옵션이 올바르지 않습니다."),
    QUIZ_SCOPE_INVALID(400, "QUIZ_SCOPE_INVALID", "출제 범위가 올바르지 않습니다."),
    QUIZ_GENERATION_IN_PROGRESS(409, "QUIZ_GENERATION_IN_PROGRESS", "이미 생성 중인 퀴즈가 있습니다."),

    // 5xx Server Errors
    INTERNAL_SERVER_ERROR(500, "C002", "서버 내부 오류가 발생했습니다."),
    SERVICE_UNAVAILABLE(503, "C009", "일시적으로 서비스를 이용할 수 없습니다."),
    MAIL_SEND_FAILED(503, "C010", "메일 전송에 실패했습니다."),
    ;

    private final int status;
    private final String code;
    private final String message;
}
