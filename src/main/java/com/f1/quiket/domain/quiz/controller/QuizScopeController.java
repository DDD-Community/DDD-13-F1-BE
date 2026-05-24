package com.f1.quiket.domain.quiz.controller;

import com.f1.quiket.domain.quiz.dto.QuizScopeResponse;
import com.f1.quiket.domain.quiz.service.QuizScopeService;
import com.f1.quiket.global.auth.UserPrincipal;
import com.f1.quiket.global.response.ApiResponse;
import com.f1.quiket.global.response.SuccessCode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 퀴즈 출제 범위 API 진입점
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/subjects")
public class QuizScopeController {

    private final QuizScopeService quizScopeService;

    /**
     * 퀴즈 출제 범위 조회
     */
    @GetMapping("/{subjectId}/quiz-scope")
    public ResponseEntity<ApiResponse<QuizScopeResponse>> getQuizScope(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable("subjectId") String subjectPublicId
    ) {
        QuizScopeResponse response = quizScopeService.getQuizScope(principal.getUserId(), subjectPublicId);
        return ResponseEntity.ok(ApiResponse.success(SuccessCode.OK, response));
    }
}
