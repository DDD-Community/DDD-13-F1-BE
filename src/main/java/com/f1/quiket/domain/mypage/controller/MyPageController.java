package com.f1.quiket.domain.mypage.controller;

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
}
