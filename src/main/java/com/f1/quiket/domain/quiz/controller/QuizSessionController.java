package com.f1.quiket.domain.quiz.controller;

import com.f1.quiket.domain.quiz.dto.QuizCreateRequest;
import com.f1.quiket.domain.quiz.dto.QuizGenerationAcceptedResponse;
import com.f1.quiket.domain.quiz.service.QuizSessionCreateService;
import com.f1.quiket.global.auth.UserPrincipal;
import com.f1.quiket.global.response.ApiResponse;
import com.f1.quiket.global.response.SuccessCode;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 퀴즈 세션 API 진입점
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/quiz-sessions")
public class QuizSessionController {

    private final QuizSessionCreateService quizSessionCreateService;

    /**
     * 퀴즈 생성 요청
     */
    @PostMapping
    public ResponseEntity<ApiResponse<QuizGenerationAcceptedResponse>> createQuizSession(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody QuizCreateRequest request
    ) {
        QuizGenerationAcceptedResponse response = quizSessionCreateService.createQuizSession(
                principal.getUserId(),
                request
        );

        return ResponseEntity
                .status(HttpStatus.ACCEPTED)
                .body(ApiResponse.success(SuccessCode.ACCEPTED, response));
    }
}
