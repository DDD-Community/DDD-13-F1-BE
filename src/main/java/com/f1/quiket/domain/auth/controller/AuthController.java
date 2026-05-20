package com.f1.quiket.domain.auth.controller;

import com.f1.quiket.domain.auth.dto.AuthTokenResponse;
import com.f1.quiket.domain.auth.dto.AuthUserResponse;
import com.f1.quiket.domain.auth.dto.EmailAvailabilityResponse;
import com.f1.quiket.domain.auth.dto.EmailVerificationConfirmRequest;
import com.f1.quiket.domain.auth.dto.EmailVerificationConfirmResponse;
import com.f1.quiket.domain.auth.dto.EmailVerificationRequest;
import com.f1.quiket.domain.auth.dto.EmailVerificationSentResponse;
import com.f1.quiket.domain.auth.dto.KakaoAccountLinkRequest;
import com.f1.quiket.domain.auth.dto.KakaoAccountLinkRequiredResponse;
import com.f1.quiket.domain.auth.dto.KakaoLoginRequest;
import com.f1.quiket.domain.auth.dto.KakaoNicknameRequest;
import com.f1.quiket.domain.auth.dto.KakaoNicknameRequiredResponse;
import com.f1.quiket.domain.auth.dto.LoginRequest;
import com.f1.quiket.domain.auth.dto.LogoutRequest;
import com.f1.quiket.domain.auth.dto.RefreshTokenRequest;
import com.f1.quiket.domain.auth.dto.SignupRequest;
import com.f1.quiket.domain.auth.dto.SignupResponse;
import com.f1.quiket.domain.auth.service.AuthTokenRequestContext;
import com.f1.quiket.domain.auth.service.AuthTokenService;
import com.f1.quiket.domain.auth.service.KakaoOAuthLoginResult;
import com.f1.quiket.domain.auth.service.KakaoOAuthService;
import com.f1.quiket.domain.auth.service.LocalAuthService;
import com.f1.quiket.global.auth.UserPrincipal;
import com.f1.quiket.global.response.ApiResponse;
import com.f1.quiket.global.response.ErrorCode;
import com.f1.quiket.global.response.SuccessCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth")
public class AuthController {

    private static final String X_FORWARDED_FOR = "X-Forwarded-For";
    private static final String X_DEVICE_ID = "X-Device-Id";
    private static final String X_DEVICE_NAME = "X-Device-Name";
    private static final String USER_AGENT = "User-Agent";

    private final LocalAuthService localAuthService;
    private final AuthTokenService authTokenService;
    private final KakaoOAuthService kakaoOAuthService;

    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<SignupResponse>> signup(@Valid @RequestBody SignupRequest request) {
        SignupResponse response = localAuthService.signup(request);
        return ResponseEntity.status(SuccessCode.AUTH_SIGNUP_SUCCESS.getStatus())
                .body(ApiResponse.success(SuccessCode.AUTH_SIGNUP_SUCCESS, response));
    }

    @GetMapping("/emails/availability")
    public ResponseEntity<ApiResponse<EmailAvailabilityResponse>> checkEmailAvailability(
            @Email(message = "올바른 이메일 형식이 아닙니다")
            @NotBlank(message = "이메일은 필수입니다")
            @RequestParam String email
    ) {
        EmailAvailabilityResponse response = localAuthService.checkEmailAvailability(email);
        SuccessCode successCode = response.isAvailable()
                ? SuccessCode.AUTH_EMAIL_AVAILABLE
                : SuccessCode.AUTH_EMAIL_UNAVAILABLE;
        return ResponseEntity.ok(ApiResponse.success(successCode, response));
    }

    @PostMapping("/email-verifications")
    public ResponseEntity<ApiResponse<EmailVerificationSentResponse>> resendEmailVerification(
            @Valid @RequestBody EmailVerificationRequest request
    ) {
        EmailVerificationSentResponse response = localAuthService.resendEmailVerification(request.getEmail());
        return ResponseEntity.ok(ApiResponse.success(SuccessCode.AUTH_EMAIL_VERIFICATION_SENT, response));
    }

    @PostMapping("/email-verifications/confirm")
    public ResponseEntity<ApiResponse<EmailVerificationConfirmResponse>> confirmEmailVerification(
            @Valid @RequestBody EmailVerificationConfirmRequest request
    ) {
        EmailVerificationConfirmResponse response = localAuthService.confirmEmailVerification(request);
        return ResponseEntity.ok(ApiResponse.success(SuccessCode.AUTH_EMAIL_VERIFIED, response));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthTokenResponse>> login(
            @Valid @RequestBody LoginRequest request,
            @RequestHeader(value = X_DEVICE_ID, required = false) String deviceId,
            @RequestHeader(value = X_DEVICE_NAME, required = false) String deviceName,
            @RequestHeader(value = USER_AGENT, required = false) String userAgent,
            HttpServletRequest httpServletRequest
    ) {
        AuthTokenResponse response = localAuthService.login(
                request,
                createTokenRequestContext(deviceId, deviceName, userAgent, httpServletRequest)
        );
        return ResponseEntity.ok(ApiResponse.success(SuccessCode.AUTH_LOGIN_SUCCESS, response));
    }

    @PostMapping("/oauth/kakao/login")
    public ResponseEntity<ApiResponse<?>> kakaoLogin(
            @Valid @RequestBody KakaoLoginRequest request,
            @RequestHeader(value = X_DEVICE_ID, required = false) String deviceId,
            @RequestHeader(value = X_DEVICE_NAME, required = false) String deviceName,
            @RequestHeader(value = USER_AGENT, required = false) String userAgent,
            HttpServletRequest httpServletRequest
    ) {
        KakaoOAuthLoginResult result = kakaoOAuthService.login(
                request,
                createTokenRequestContext(deviceId, deviceName, userAgent, httpServletRequest)
        );
        return switch (result.getStatus()) {
            case EXISTING_LOGIN -> ResponseEntity.ok(
                    ApiResponse.success(SuccessCode.AUTH_OAUTH_LOGIN_SUCCESS, result.getTokenResponse())
            );
            case SIGNUP_LOGIN -> ResponseEntity.status(SuccessCode.AUTH_OAUTH_SIGNUP_SUCCESS.getStatus())
                    .body(ApiResponse.success(SuccessCode.AUTH_OAUTH_SIGNUP_SUCCESS, result.getTokenResponse()));
            case ACCOUNT_LINK_REQUIRED -> accountLinkRequiredResponse(result.getAccountLinkRequiredResponse());
            case NICKNAME_REQUIRED -> ResponseEntity.status(SuccessCode.AUTH_NICKNAME_REQUIRED.getStatus())
                    .body(ApiResponse.success(SuccessCode.AUTH_NICKNAME_REQUIRED, result.getNicknameRequiredResponse()));
        };
    }

    @PostMapping("/oauth/kakao/link")
    public ResponseEntity<ApiResponse<AuthTokenResponse>> linkKakaoAccount(
            @Valid @RequestBody KakaoAccountLinkRequest request,
            @RequestHeader(value = X_DEVICE_ID, required = false) String deviceId,
            @RequestHeader(value = X_DEVICE_NAME, required = false) String deviceName,
            @RequestHeader(value = USER_AGENT, required = false) String userAgent,
            HttpServletRequest httpServletRequest
    ) {
        AuthTokenResponse response = kakaoOAuthService.link(
                request,
                createTokenRequestContext(deviceId, deviceName, userAgent, httpServletRequest)
        );
        return ResponseEntity.ok(ApiResponse.success(SuccessCode.AUTH_OAUTH_LINK_SUCCESS, response));
    }

    @PostMapping("/oauth/kakao/nickname")
    public ResponseEntity<ApiResponse<AuthTokenResponse>> completeKakaoNickname(
            @Valid @RequestBody KakaoNicknameRequest request,
            @RequestHeader(value = X_DEVICE_ID, required = false) String deviceId,
            @RequestHeader(value = X_DEVICE_NAME, required = false) String deviceName,
            @RequestHeader(value = USER_AGENT, required = false) String userAgent,
            HttpServletRequest httpServletRequest
    ) {
        AuthTokenResponse response = kakaoOAuthService.completeNickname(
                request,
                createTokenRequestContext(deviceId, deviceName, userAgent, httpServletRequest)
        );
        return ResponseEntity.ok(ApiResponse.success(SuccessCode.AUTH_OAUTH_SIGNUP_SUCCESS, response));
    }

    @PostMapping("/token/refresh")
    public ResponseEntity<ApiResponse<AuthTokenResponse>> refreshToken(
            @Valid @RequestBody RefreshTokenRequest request,
            @RequestHeader(value = X_DEVICE_ID, required = false) String deviceId,
            @RequestHeader(value = X_DEVICE_NAME, required = false) String deviceName,
            @RequestHeader(value = USER_AGENT, required = false) String userAgent,
            HttpServletRequest httpServletRequest
    ) {
        AuthTokenResponse response = authTokenService.refresh(
                request,
                createTokenRequestContext(deviceId, deviceName, userAgent, httpServletRequest)
        );
        return ResponseEntity.ok(ApiResponse.success(SuccessCode.AUTH_TOKEN_REFRESHED, response));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody(required = false) LogoutRequest request
    ) {
        authTokenService.logout(principal.getPublicId(), request);
        return ResponseEntity.ok(ApiResponse.success(SuccessCode.AUTH_LOGOUT_SUCCESS));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<AuthUserResponse>> getMe(@AuthenticationPrincipal UserPrincipal principal) {
        AuthUserResponse response = authTokenService.getMe(principal.getPublicId());
        return ResponseEntity.ok(ApiResponse.success(SuccessCode.AUTH_ME_SUCCESS, response));
    }

    private AuthTokenRequestContext createTokenRequestContext(
            String deviceId,
            String deviceName,
            String userAgent,
            HttpServletRequest request
    ) {
        return AuthTokenRequestContext.builder()
                .deviceId(deviceId)
                .deviceName(deviceName)
                .userAgent(userAgent)
                .ipAddress(resolveClientIp(request))
                .build();
    }

    private String resolveClientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader(X_FORWARDED_FOR);
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private ResponseEntity<ApiResponse<?>> accountLinkRequiredResponse(KakaoAccountLinkRequiredResponse response) {
        return ResponseEntity.status(ErrorCode.AUTH_OAUTH_ACCOUNT_LINK_REQUIRED.getStatus())
                .body(ApiResponse.<KakaoAccountLinkRequiredResponse>builder()
                        .success(false)
                        .code(ErrorCode.AUTH_OAUTH_ACCOUNT_LINK_REQUIRED.getCode())
                        .message(ErrorCode.AUTH_OAUTH_ACCOUNT_LINK_REQUIRED.getMessage())
                        .data(response)
                        .build());
    }
}
