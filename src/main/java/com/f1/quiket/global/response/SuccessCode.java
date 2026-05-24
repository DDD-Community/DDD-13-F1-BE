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

    // Auth Success
    AUTH_SIGNUP_SUCCESS(201, "AUTH_SIGNUP_SUCCESS", "회원가입이 완료되었습니다. 이메일 인증을 진행해주세요."),
    AUTH_EMAIL_AVAILABLE(200, "AUTH_EMAIL_AVAILABLE", "사용 가능한 이메일입니다."),
    AUTH_EMAIL_UNAVAILABLE(200, "AUTH_EMAIL_UNAVAILABLE", "이미 사용 중인 이메일입니다."),
    AUTH_EMAIL_VERIFICATION_SENT(200, "AUTH_EMAIL_VERIFICATION_SENT", "이메일 인증 메일이 발송되었습니다."),
    AUTH_EMAIL_VERIFIED(200, "AUTH_EMAIL_VERIFIED", "이메일 인증이 완료되었습니다."),
    AUTH_EMAIL_VERIFIED_AND_LOGIN(200, "AUTH_EMAIL_VERIFIED_AND_LOGIN", "이메일 인증이 완료되었습니다."),
    AUTH_PASSWORD_RESET_REQUESTED(200, "AUTH_PASSWORD_RESET_REQUESTED", "비밀번호 재설정 안내가 발송되었습니다."),
    AUTH_PASSWORD_RESET_COMPLETED(200, "AUTH_PASSWORD_RESET_COMPLETED", "비밀번호 재설정이 완료되었습니다."),
    AUTH_LOGIN_SUCCESS(200, "AUTH_LOGIN_SUCCESS", "로그인이 완료되었습니다."),
    AUTH_TOKEN_REFRESHED(200, "AUTH_TOKEN_REFRESHED", "토큰이 재발급되었습니다."),
    AUTH_LOGOUT_SUCCESS(200, "AUTH_LOGOUT_SUCCESS", "로그아웃이 완료되었습니다."),
    AUTH_ME_SUCCESS(200, "AUTH_ME_SUCCESS", "내 정보 조회가 완료되었습니다."),
    AUTH_OAUTH_LOGIN_SUCCESS(200, "AUTH_OAUTH_LOGIN_SUCCESS", "Kakao 로그인이 완료되었습니다."),
    AUTH_OAUTH_SIGNUP_SUCCESS(201, "AUTH_OAUTH_SIGNUP_SUCCESS", "Kakao 회원가입 및 로그인이 완료되었습니다."),
    AUTH_OAUTH_LINK_SUCCESS(200, "AUTH_OAUTH_LINK_SUCCESS", "Kakao 계정 연동 및 로그인이 완료되었습니다."),
    AUTH_OAUTH_NICKNAME_COMPLETED(200, "AUTH_OAUTH_NICKNAME_COMPLETED", "닉네임 설정 및 로그인이 완료되었습니다."),
    AUTH_NICKNAME_REQUIRED(202, "AUTH_NICKNAME_REQUIRED", "닉네임 설정이 필요합니다."),
    ;

    private final int status;
    private final String code;
    private final String message;
}
