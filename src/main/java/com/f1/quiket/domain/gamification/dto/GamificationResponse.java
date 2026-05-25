package com.f1.quiket.domain.gamification.dto;

import com.f1.quiket.domain.gamification.service.GamificationLevelCalculator;
import com.f1.quiket.domain.user.entity.User;
import com.f1.quiket.domain.user.entity.type.UserLevel;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class GamificationResponse {

    private final Integer dotoriBalance;
    private final Integer xpTotal;
    private final Integer currentLevel;
    private final String currentLevelName;
    private final Integer maxLevel;
    private final Integer nextLevel;
    private final String nextLevelName;
    private final Integer currentLevelMinXp;
    private final Integer nextLevelRequiredXp;
    private final Integer levelProgressPct;

    public static GamificationResponse from(User user) {
        Integer nextLevel = GamificationLevelCalculator.nextLevel(user.getCurrentLevel());
        return GamificationResponse.builder()
                .dotoriBalance(user.getDotoriBalance())
                .xpTotal(user.getXpTotal())
                .currentLevel(user.getCurrentLevel())
                .currentLevelName(UserLevel.titleOf(user.getCurrentLevel()))
                .maxLevel(GamificationLevelCalculator.MAX_LEVEL)
                .nextLevel(nextLevel)
                .nextLevelName(nextLevel == null ? null : UserLevel.titleOf(nextLevel))
                .currentLevelMinXp(GamificationLevelCalculator.currentLevelMinXp(user.getCurrentLevel()))
                .nextLevelRequiredXp(GamificationLevelCalculator.nextLevelRequiredXp(user.getCurrentLevel()))
                .levelProgressPct(GamificationLevelCalculator.progressPct(user.getXpTotal(), user.getCurrentLevel()))
                .build();
    }
}
