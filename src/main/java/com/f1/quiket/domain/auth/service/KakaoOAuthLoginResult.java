package com.f1.quiket.domain.auth.service;

import com.f1.quiket.domain.auth.dto.AuthTokenResponse;
import com.f1.quiket.domain.auth.dto.KakaoAccountLinkRequiredResponse;
import com.f1.quiket.domain.auth.dto.KakaoNicknameRequiredResponse;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class KakaoOAuthLoginResult {

    private final KakaoOAuthLoginStatus status;
    private final AuthTokenResponse tokenResponse;
    private final KakaoAccountLinkRequiredResponse accountLinkRequiredResponse;
    private final KakaoNicknameRequiredResponse nicknameRequiredResponse;

    public static KakaoOAuthLoginResult existingLogin(AuthTokenResponse tokenResponse) {
        return new KakaoOAuthLoginResult(
                KakaoOAuthLoginStatus.EXISTING_LOGIN,
                tokenResponse,
                null,
                null
        );
    }

    public static KakaoOAuthLoginResult signupLogin(AuthTokenResponse tokenResponse) {
        return new KakaoOAuthLoginResult(
                KakaoOAuthLoginStatus.SIGNUP_LOGIN,
                tokenResponse,
                null,
                null
        );
    }

    public static KakaoOAuthLoginResult accountLinkRequired(KakaoAccountLinkRequiredResponse response) {
        return new KakaoOAuthLoginResult(
                KakaoOAuthLoginStatus.ACCOUNT_LINK_REQUIRED,
                null,
                response,
                null
        );
    }

    public static KakaoOAuthLoginResult nicknameRequired(KakaoNicknameRequiredResponse response) {
        return new KakaoOAuthLoginResult(
                KakaoOAuthLoginStatus.NICKNAME_REQUIRED,
                null,
                null,
                response
        );
    }
}
