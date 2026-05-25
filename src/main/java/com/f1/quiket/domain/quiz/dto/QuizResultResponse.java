package com.f1.quiket.domain.quiz.dto;

import com.f1.quiket.domain.quiz.entity.QuizPlaySession;
import com.f1.quiket.domain.quiz.entity.QuizResult;
import com.f1.quiket.domain.quiz.entity.QuizSession;
import com.f1.quiket.domain.subject.entity.Subject;
import com.f1.quiket.domain.user.entity.User;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class QuizResultResponse {

    private final String resultId;
    private final String playSessionId;
    private final String quizSessionId;
    private final String subjectId;
    private final String subjectName;
    private final Integer totalCount;
    private final Integer correctCount;
    private final Integer wrongCount;
    private final Integer skipCount;
    private final Integer accuracyPct;
    private final Integer elapsedMs;
    private final Boolean scoreMatched;
    private final Boolean abuseFlagged;
    private final QuizRewardSummaryResponse rewards;
    private final List<QuizReviewItemResponse> reviewItems;
    private final QuizRetryAvailableResponse retryAvailable;
    private final LocalDateTime createdAt;

    public static QuizResultResponse of(
            QuizResult result,
            QuizPlaySession playSession,
            QuizSession quizSession,
            Subject subject,
            User user,
            List<QuizReviewItemResponse> reviewItems
    ) {
        return QuizResultResponse.builder()
                .resultId(result.getPublicId())
                .playSessionId(playSession.getClientSessionId())
                .quizSessionId(quizSession.getPublicId())
                .subjectId(subject.getPublicId())
                .subjectName(subject.getName())
                .totalCount(result.getTotalCount())
                .correctCount(result.getCorrectCount())
                .wrongCount(result.getWrongCount())
                .skipCount(result.getSkipCount())
                .accuracyPct(result.getAccuracyPct())
                .elapsedMs(result.getElapsedMs())
                .scoreMatched(result.getScoreMatched())
                .abuseFlagged(result.getAbuseFlagged())
                .rewards(QuizRewardSummaryResponse.of(result, user))
                .reviewItems(reviewItems)
                .retryAvailable(QuizRetryAvailableResponse.from(result.getWrongCount()))
                .createdAt(result.getCreatedAt())
                .build();
    }
}
