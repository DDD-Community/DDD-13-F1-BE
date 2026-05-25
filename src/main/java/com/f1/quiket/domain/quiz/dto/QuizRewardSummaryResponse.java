package com.f1.quiket.domain.quiz.dto;

import com.f1.quiket.domain.quiz.entity.QuizResult;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class QuizRewardSummaryResponse {

    private final Integer dotoriEarned;
    private final Integer xpEarned;
    private final Boolean leveledUp;
    private final Integer newLevel;

    public static QuizRewardSummaryResponse from(QuizResult result) {
        return QuizRewardSummaryResponse.builder()
                .dotoriEarned(result.getDotoriEarned())
                .xpEarned(result.getXpEarned())
                .leveledUp(result.getLeveledUp())
                .newLevel(result.getNewLevel())
                .build();
    }
}
