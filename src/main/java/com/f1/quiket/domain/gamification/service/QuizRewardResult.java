package com.f1.quiket.domain.gamification.service;

public record QuizRewardResult(
        Integer dotoriEarned,
        Integer xpEarned,
        Boolean leveledUp,
        Integer newLevel,
        Integer currentDotoriBalance,
        Integer currentXpTotal
) {
}
