package com.f1.quiket.domain.mypage.controller;

import com.f1.quiket.domain.auth.dto.EmailVerificationSentResponse;
import com.f1.quiket.domain.mypage.dto.MyAccountDeleteRequest;
import com.f1.quiket.domain.mypage.dto.MyEmailChangeConfirmRequest;
import com.f1.quiket.domain.mypage.dto.MyEmailChangeRequest;
import com.f1.quiket.domain.mypage.dto.MyPasswordChangeRequest;
import com.f1.quiket.domain.mypage.dto.MyProfileResponse;
import com.f1.quiket.domain.mypage.dto.NicknameUpdateRequest;
import com.f1.quiket.domain.mypage.service.MyPageService;
import com.f1.quiket.global.auth.UserPrincipal;
import com.f1.quiket.global.response.ApiResponse;
import com.f1.quiket.global.response.SuccessCode;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 마이페이지 API 진입점
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/my")
public class MyPageController {

    private final MyPageService myPageService;

    /**
     * 마이페이지 계정 정보 조회
     */
    @GetMapping("/profile")
    public ResponseEntity<ApiResponse<MyProfileResponse>> getMyProfile(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        MyProfileResponse response = myPageService.getMyProfile(principal.getPublicId());
        return ResponseEntity.ok(ApiResponse.success(SuccessCode.OK, response));
    }

    /**
     * 닉네임 변경
     */
    @PatchMapping("/profile/nickname")
    public ResponseEntity<ApiResponse<MyProfileResponse>> updateNickname(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody NicknameUpdateRequest request
    ) {
        MyProfileResponse response = myPageService.updateNickname(principal.getPublicId(), request);
        return ResponseEntity.ok(ApiResponse.success(SuccessCode.OK, response));
    }

    @PostMapping("/email/change-requests")
    public ResponseEntity<ApiResponse<EmailVerificationSentResponse>> requestEmailChange(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody MyEmailChangeRequest request
    ) {
        EmailVerificationSentResponse response = myPageService.requestEmailChange(principal.getPublicId(), request);
        return ResponseEntity.ok(ApiResponse.success(SuccessCode.OK, response));
    }

    @PostMapping("/email/change-confirm")
    public ResponseEntity<ApiResponse<MyProfileResponse>> confirmEmailChange(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody MyEmailChangeConfirmRequest request
    ) {
        MyProfileResponse response = myPageService.confirmEmailChange(principal.getPublicId(), request);
        return ResponseEntity.ok(ApiResponse.success(SuccessCode.OK, response));
    }

    /**
     * 비밀번호 변경
     */
    @PatchMapping("/password")
    public ResponseEntity<ApiResponse<Void>> updatePassword(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody MyPasswordChangeRequest request
    ) {
        myPageService.updatePassword(principal.getPublicId(), request);
        return ResponseEntity.ok(ApiResponse.success(SuccessCode.OK));
    }

    @PostMapping("/account/deletion")
    public ResponseEntity<ApiResponse<Void>> deleteMyAccount(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody MyAccountDeleteRequest request
    ) {
        myPageService.deleteAccount(principal.getPublicId(), request);
        return ResponseEntity.ok(ApiResponse.success(SuccessCode.OK));
    }
}
