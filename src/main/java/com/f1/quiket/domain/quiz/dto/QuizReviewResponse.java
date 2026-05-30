package com.f1.quiket.domain.quiz.dto;

import java.util.List;
import lombok.Builder;
import lombok.Getter;

/**
 * 문제별 해설(리뷰) 조회 응답 — RESULT-002 해설보기.
 */
@Getter
@Builder
public class QuizReviewResponse {

    private final String playSessionId;
    private final List<QuizReviewItemResponse> items;

    public static QuizReviewResponse of(String playSessionId, List<QuizReviewItemResponse> items) {
        return QuizReviewResponse.builder()
                .playSessionId(playSessionId)
                .items(items)
                .build();
    }
}
