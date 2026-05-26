package com.f1.quiket.domain.gamification.controller;

import com.f1.quiket.domain.gamification.dto.GamificationResponse;
import com.f1.quiket.domain.gamification.service.GamificationService;
import com.f1.quiket.global.auth.UserPrincipal;
import com.f1.quiket.global.response.ApiResponse;
import com.f1.quiket.global.response.SuccessCode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/gamification")
public class GamificationController {

    private final GamificationService gamificationService;

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<GamificationResponse>> getMyGamification(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        GamificationResponse response = gamificationService.getMyGamification(principal.getPublicId());
        return ResponseEntity.ok(ApiResponse.success(SuccessCode.OK, response));
    }
}
