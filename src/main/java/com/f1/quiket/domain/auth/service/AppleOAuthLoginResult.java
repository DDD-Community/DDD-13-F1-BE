package com.f1.quiket.domain.auth.service;

import com.f1.quiket.domain.auth.dto.AppleAccountLinkRequiredResponse;
import com.f1.quiket.domain.auth.dto.AppleNicknameRequiredResponse;
import com.f1.quiket.domain.auth.dto.AuthTokenResponse;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class AppleOAuthLoginResult {

    private final AppleOAuthLoginStatus status;
    private final AuthTokenResponse tokenResponse;
    private final AppleAccountLinkRequiredResponse accountLinkRequiredResponse;
    private final AppleNicknameRequiredResponse nicknameRequiredResponse;

    public static AppleOAuthLoginResult existingLogin(AuthTokenResponse tokenResponse) {
        return new AppleOAuthLoginResult(
                AppleOAuthLoginStatus.EXISTING_LOGIN,
                tokenResponse,
                null,
                null
        );
    }

    public static AppleOAuthLoginResult signupLogin(AuthTokenResponse tokenResponse) {
        return new AppleOAuthLoginResult(
                AppleOAuthLoginStatus.SIGNUP_LOGIN,
                tokenResponse,
                null,
                null
        );
    }

    public static AppleOAuthLoginResult accountLinkRequired(AppleAccountLinkRequiredResponse response) {
        return new AppleOAuthLoginResult(
                AppleOAuthLoginStatus.ACCOUNT_LINK_REQUIRED,
                null,
                response,
                null
        );
    }

    public static AppleOAuthLoginResult nicknameRequired(AppleNicknameRequiredResponse response) {
        return new AppleOAuthLoginResult(
                AppleOAuthLoginStatus.NICKNAME_REQUIRED,
                null,
                null,
                response
        );
    }
}
