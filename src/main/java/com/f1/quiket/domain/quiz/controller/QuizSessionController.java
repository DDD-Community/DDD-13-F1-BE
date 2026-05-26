package com.f1.quiket.domain.quiz.controller;

import com.f1.quiket.domain.quiz.dto.QuizCreateRequest;
import com.f1.quiket.domain.quiz.dto.QuizGenerationAcceptedResponse;
import com.f1.quiket.domain.quiz.dto.QuizGenerationStatusResponse;
import com.f1.quiket.domain.quiz.dto.QuizPlaySessionResponse;
import com.f1.quiket.domain.quiz.dto.QuizPlayStartRequest;
import com.f1.quiket.domain.quiz.dto.QuizSessionResponse;
import com.f1.quiket.domain.quiz.service.QuizGenerationStatusService;
import com.f1.quiket.domain.quiz.service.QuizPlaySessionStartService;
import com.f1.quiket.domain.quiz.service.QuizSessionCreateService;
import com.f1.quiket.domain.quiz.service.QuizSessionQueryService;
import com.f1.quiket.global.auth.UserPrincipal;
import com.f1.quiket.global.response.ApiResponse;
import com.f1.quiket.global.response.SuccessCode;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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
    private final QuizGenerationStatusService quizGenerationStatusService;
    private final QuizSessionQueryService quizSessionQueryService;
    private final QuizPlaySessionStartService quizPlaySessionStartService;

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

    /**
     * 퀴즈 생성 상태 조회
     */
    @GetMapping("/{quizSessionId}/generation-status")
    public ResponseEntity<ApiResponse<QuizGenerationStatusResponse>> getGenerationStatus(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable("quizSessionId") String quizSessionPublicId
    ) {
        QuizGenerationStatusResponse response = quizGenerationStatusService.getGenerationStatus(
                principal.getUserId(),
                quizSessionPublicId
        );

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.success(SuccessCode.OK, response));
    }

    /**
     * 퀴즈 세트 조회
     */
    @GetMapping("/{quizSessionId}")
    public ResponseEntity<ApiResponse<QuizSessionResponse>> getQuizSession(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable("quizSessionId") String quizSessionPublicId
    ) {
        QuizSessionResponse response = quizSessionQueryService.getQuizSession(
                principal.getUserId(),
                quizSessionPublicId
        );

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(ApiResponse.success(SuccessCode.OK, response));
    }

    /**
     * 퀴즈 풀이 세션 시작
     */
    @PostMapping("/{quizSessionId}/play-sessions")
    public ResponseEntity<ApiResponse<QuizPlaySessionResponse>> startPlaySession(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable("quizSessionId") String quizSessionPublicId,
            @Valid @RequestBody QuizPlayStartRequest request
    ) {
        QuizPlaySessionResponse response = quizPlaySessionStartService.start(
                principal.getUserId(),
                quizSessionPublicId,
                request
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(SuccessCode.CREATED, response));
    }
}
