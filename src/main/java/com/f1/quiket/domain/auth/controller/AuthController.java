package com.f1.quiket.domain.auth.controller;

import com.f1.quiket.domain.auth.dto.EmailAvailabilityResponse;
import com.f1.quiket.domain.auth.dto.EmailVerificationConfirmRequest;
import com.f1.quiket.domain.auth.dto.EmailVerificationConfirmResponse;
import com.f1.quiket.domain.auth.dto.EmailVerificationRequest;
import com.f1.quiket.domain.auth.dto.EmailVerificationSentResponse;
import com.f1.quiket.domain.auth.dto.LocalLoginResponse;
import com.f1.quiket.domain.auth.dto.LoginRequest;
import com.f1.quiket.domain.auth.dto.SignupRequest;
import com.f1.quiket.domain.auth.dto.SignupResponse;
import com.f1.quiket.domain.auth.service.LocalAuthService;
import com.f1.quiket.global.response.ApiResponse;
import com.f1.quiket.global.response.SuccessCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth")
public class AuthController {

    private static final String X_FORWARDED_FOR = "X-Forwarded-For";

    private final LocalAuthService localAuthService;

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
    public ResponseEntity<ApiResponse<LocalLoginResponse>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpServletRequest
    ) {
        LocalLoginResponse response = localAuthService.login(request, resolveClientIp(httpServletRequest));
        return ResponseEntity.ok(ApiResponse.success(SuccessCode.AUTH_LOGIN_SUCCESS, response));
    }

    private String resolveClientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader(X_FORWARDED_FOR);
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
