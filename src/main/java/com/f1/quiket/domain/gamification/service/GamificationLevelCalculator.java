package com.f1.quiket.domain.gamification.service;

/**
 * 게임화 레벨 계산 유틸 — GAMIF-002 캐릭터 성장 & XP 정책 기준.
 *
 * <p>총 10레벨(만렙) 구조이며 레벨별 누적 XP 하한은 명세를 그대로 옮긴 값이다.
 * 만렙(Lv.10) 도달 후엔 추가 XP가 누적되지 않는 정책이라 적립측에서
 * {@link #MAX_LEVEL_MIN_XP}를 상한으로 사용한다.</p>
 */
public final class GamificationLevelCalculator {

    /**
     * 명세 GAMIF-002 — 만렙 차수.
     */
    public static final int MAX_LEVEL = 10;

    /**
     * 명세 GAMIF-002 — 레벨별 누적 XP 하한(레벨 시작값).
     * <ul>
     *     <li>Lv.1: 0 ~ 99</li>
     *     <li>Lv.2: 100 ~ 349</li>
     *     <li>Lv.3: 350 ~ 849</li>
     *     <li>Lv.4: 850 ~ 1,749</li>
     *     <li>Lv.5: 1,750 ~ 3,249</li>
     *     <li>Lv.6: 3,250 ~ 5,749</li>
     *     <li>Lv.7: 5,750 ~ 9,749</li>
     *     <li>Lv.8: 9,750 ~ 16,249</li>
     *     <li>Lv.9: 16,250 ~ 26,249</li>
     *     <li>Lv.10: 26,250 이상 (만렙)</li>
     * </ul>
     */
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

    /**
     * 명세 GAMIF-002 — 만렙(Lv.10) 진입 누적 XP. 누적 XP가 이 값 이상이면 만렙이며,
     * 만렙 도달 후엔 추가 XP를 누적하지 않는다.
     */
    public static final int MAX_LEVEL_MIN_XP = LEVEL_MIN_XP[MAX_LEVEL - 1];

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
