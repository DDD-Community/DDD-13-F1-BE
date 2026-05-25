package com.f1.quiket.domain.quiz.dto;

import com.f1.quiket.domain.quiz.entity.QuizResult;
import com.f1.quiket.domain.user.entity.User;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class QuizRewardSummaryResponse {

    private final Integer dotoriEarned;
    private final Integer xpEarned;
    private final Boolean leveledUp;
    private final Integer newLevel;
    private final Integer currentDotoriBalance;
    private final Integer currentXpTotal;

    public static QuizRewardSummaryResponse of(QuizResult result, User user) {
        return QuizRewardSummaryResponse.builder()
                .dotoriEarned(result.getDotoriEarned())
                .xpEarned(result.getXpEarned())
                .leveledUp(result.getLeveledUp())
                .newLevel(result.getNewLevel())
                .currentDotoriBalance(user.getDotoriBalance())
                .currentXpTotal(user.getXpTotal())
                .build();
    }
}
