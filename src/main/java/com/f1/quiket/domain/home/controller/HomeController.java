package com.f1.quiket.domain.home.controller;

import com.f1.quiket.domain.home.dto.HomeDataResponse;
import com.f1.quiket.domain.home.dto.RecentActivityPageResponse;
import com.f1.quiket.domain.home.service.HomeService;
import com.f1.quiket.global.auth.UserPrincipal;
import com.f1.quiket.global.response.ApiResponse;
import com.f1.quiket.global.response.SuccessCode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 홈 화면 API 진입점
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/home")
public class HomeController {

    private final HomeService homeService;

    /**
     * 홈 메인 데이터 조회
     */
    @GetMapping
    public ResponseEntity<ApiResponse<HomeDataResponse>> getHome(@AuthenticationPrincipal UserPrincipal principal) {
        HomeDataResponse response = homeService.getHome(principal.getPublicId());
        return ResponseEntity.ok(ApiResponse.success(SuccessCode.OK, response));
    }

    /**
     * 홈 최근활동 목록 조회
     */
    @GetMapping("/recent-activities")
    public ResponseEntity<ApiResponse<RecentActivityPageResponse>> getRecentActivities(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(name="page", defaultValue = "0") int page,
            @RequestParam(name="size", defaultValue = "10") int size
    ) {
        RecentActivityPageResponse response = homeService.getRecentActivities(principal.getPublicId(), page, size);
        return ResponseEntity.ok(ApiResponse.success(SuccessCode.OK, response));
    }
}
