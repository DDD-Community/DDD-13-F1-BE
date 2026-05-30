package com.f1.quiket.domain.quiz.controller;

import com.f1.quiket.domain.quiz.dto.QuizPlaySessionResponse;
import com.f1.quiket.domain.quiz.dto.QuizResultResponse;
import com.f1.quiket.domain.quiz.dto.QuizResultSubmitOutcome;
import com.f1.quiket.domain.quiz.dto.QuizResultSubmitRequest;
import com.f1.quiket.domain.quiz.dto.QuizRetryRequest;
import com.f1.quiket.domain.quiz.dto.QuizReviewResponse;
import com.f1.quiket.domain.quiz.service.QuizResultQueryService;
import com.f1.quiket.domain.quiz.service.QuizResultRetryAllService;
import com.f1.quiket.domain.quiz.service.QuizResultRetryWrongService;
import com.f1.quiket.domain.quiz.service.QuizResultSubmitService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 퀴즈 결과 API 진입점
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/quiz-results")
public class QuizResultController {

    private final QuizResultSubmitService quizResultSubmitService;
    private final QuizResultQueryService quizResultQueryService;
    private final QuizResultRetryAllService quizResultRetryAllService;
    private final QuizResultRetryWrongService quizResultRetryWrongService;

    /**
     * 퀴즈 결과 제출
     */
    @PostMapping
    public ResponseEntity<ApiResponse<QuizResultResponse>> submitQuizResult(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody QuizResultSubmitRequest request
    ) {
        QuizResultSubmitOutcome outcome = quizResultSubmitService.submit(principal.getUserId(), request);

        return ResponseEntity
                .status(outcome.isCreated() ? HttpStatus.CREATED : HttpStatus.OK)
                .body(ApiResponse.success(outcome.isCreated() ? SuccessCode.CREATED : SuccessCode.OK, outcome.getResponse()));
    }

    /**
     * 과거 퀴즈 결과 상세 조회
     */
    @GetMapping("/{resultId}")
    public ResponseEntity<ApiResponse<QuizResultResponse>> getQuizResult(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable("resultId") String resultPublicId
    ) {
        QuizResultResponse response = quizResultQueryService.getQuizResult(principal.getUserId(), resultPublicId);
        return ResponseEntity.ok(ApiResponse.success(SuccessCode.OK, response));
    }

    /**
     * 문제별 해설 조회 (RESULT-002, filter: all/correct/wrong)
     */
    @GetMapping("/{resultId}/review")
    public ResponseEntity<ApiResponse<QuizReviewResponse>> getQuizResultReview(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable("resultId") String resultPublicId,
            @RequestParam(value = "filter", required = false, defaultValue = "all") String filter
    ) {
        QuizReviewResponse response = quizResultQueryService.getQuizReview(
                principal.getUserId(),
                resultPublicId,
                filter
        );
        return ResponseEntity.ok(ApiResponse.success(SuccessCode.OK, response));
    }

    /**
     * 전체 다시풀기 시작
     */
    @PostMapping("/{resultId}/retry-all")
    public ResponseEntity<ApiResponse<QuizPlaySessionResponse>> retryAllQuestions(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable("resultId") String resultPublicId,
            @Valid @RequestBody QuizRetryRequest request
    ) {
        QuizPlaySessionResponse response = quizResultRetryAllService.retryAll(
                principal.getUserId(),
                resultPublicId,
                request
        );
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(SuccessCode.CREATED, response));
    }

    /**
     * 틀린 문제만 다시풀기 시작
     */
    @PostMapping("/{resultId}/retry-wrong")
    public ResponseEntity<ApiResponse<QuizPlaySessionResponse>> retryWrongQuestions(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable("resultId") String resultPublicId,
            @Valid @RequestBody QuizRetryRequest request
    ) {
        QuizPlaySessionResponse response = quizResultRetryWrongService.retryWrong(
                principal.getUserId(),
                resultPublicId,
                request
        );
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(SuccessCode.CREATED, response));
    }
}
