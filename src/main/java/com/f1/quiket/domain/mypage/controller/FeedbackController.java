package com.f1.quiket.domain.mypage.controller;

import com.f1.quiket.domain.mypage.dto.FeedbackCreateRequest;
import com.f1.quiket.domain.mypage.dto.FeedbackResponse;
import com.f1.quiket.domain.mypage.service.FeedbackService;
import com.f1.quiket.global.auth.UserPrincipal;
import com.f1.quiket.global.response.ApiResponse;
import com.f1.quiket.global.response.SuccessCode;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/feedbacks")
public class FeedbackController {

    private final FeedbackService feedbackService;

    @PostMapping
    public ResponseEntity<ApiResponse<FeedbackResponse>> createFeedback(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody FeedbackCreateRequest request
    ) {
        FeedbackResponse response = feedbackService.createFeedback(principal.getPublicId(), request);
        return ResponseEntity.status(SuccessCode.CREATED.getStatus())
                .body(ApiResponse.success(SuccessCode.CREATED, response));
    }
}
