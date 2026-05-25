package com.f1.quiket.domain.gamification.service;

public final class GamificationLevelCalculator {

    public static final int MAX_LEVEL = 10;

    private static final int[] LEVEL_MIN_XP = {
            0,
            100,
            350,
            850,
            1750,
            3250,
            5750,
            9750,
            16250,
            26250
    };

    private GamificationLevelCalculator() {
    }

    public static int levelOf(Integer xpTotal) {
        int normalizedXp = normalizeXp(xpTotal);
        for (int index = LEVEL_MIN_XP.length - 1; index >= 0; index--) {
            if (normalizedXp >= LEVEL_MIN_XP[index]) {
                return index + 1;
            }
        }
        return 1;
    }

    public static int currentLevelMinXp(Integer currentLevel) {
        return LEVEL_MIN_XP[normalizeLevel(currentLevel) - 1];
    }

    public static Integer nextLevel(Integer currentLevel) {
        int normalizedLevel = normalizeLevel(currentLevel);
        if (normalizedLevel >= MAX_LEVEL) {
            return null;
        }
        return normalizedLevel + 1;
    }

    public static Integer nextLevelRequiredXp(Integer currentLevel) {
        Integer nextLevel = nextLevel(currentLevel);
        if (nextLevel == null) {
            return null;
        }
        return LEVEL_MIN_XP[nextLevel - 1];
    }

    public static int progressPct(Integer xpTotal, Integer currentLevel) {
        int normalizedLevel = normalizeLevel(currentLevel);
        if (normalizedLevel >= MAX_LEVEL) {
            return 100;
        }

        int minXp = currentLevelMinXp(normalizedLevel);
        int nextMinXp = nextLevelRequiredXp(normalizedLevel);
        int progressPct = (normalizeXp(xpTotal) - minXp) * 100 / (nextMinXp - minXp);
        return Math.max(0, Math.min(progressPct, 100));
    }

    private static int normalizeXp(Integer xpTotal) {
        return Math.max(xpTotal == null ? 0 : xpTotal, 0);
    }

    private static int normalizeLevel(Integer currentLevel) {
        if (currentLevel == null || currentLevel < 1) {
            return 1;
        }
        return Math.min(currentLevel, MAX_LEVEL);
    }
}
