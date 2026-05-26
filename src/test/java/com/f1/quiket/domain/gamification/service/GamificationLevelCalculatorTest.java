package com.f1.quiket.domain.gamification.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * 명세 GAMIF-002 — 레벨 구간 경계값과 만렙 진입선이 정책과 일치하는지 단언한다.
 */
class GamificationLevelCalculatorTest {

    @Test
    void levelOf_maps_each_band_min_xp_to_expected_level() {
        assertThat(GamificationLevelCalculator.levelOf(0)).isEqualTo(1);
        assertThat(GamificationLevelCalculator.levelOf(99)).isEqualTo(1);
        assertThat(GamificationLevelCalculator.levelOf(100)).isEqualTo(2);
        assertThat(GamificationLevelCalculator.levelOf(349)).isEqualTo(2);
        assertThat(GamificationLevelCalculator.levelOf(350)).isEqualTo(3);
        assertThat(GamificationLevelCalculator.levelOf(849)).isEqualTo(3);
        assertThat(GamificationLevelCalculator.levelOf(850)).isEqualTo(4);
        assertThat(GamificationLevelCalculator.levelOf(1749)).isEqualTo(4);
        assertThat(GamificationLevelCalculator.levelOf(1750)).isEqualTo(5);
        assertThat(GamificationLevelCalculator.levelOf(3249)).isEqualTo(5);
        assertThat(GamificationLevelCalculator.levelOf(3250)).isEqualTo(6);
        assertThat(GamificationLevelCalculator.levelOf(5749)).isEqualTo(6);
        assertThat(GamificationLevelCalculator.levelOf(5750)).isEqualTo(7);
        assertThat(GamificationLevelCalculator.levelOf(9749)).isEqualTo(7);
        assertThat(GamificationLevelCalculator.levelOf(9750)).isEqualTo(8);
        assertThat(GamificationLevelCalculator.levelOf(16249)).isEqualTo(8);
        assertThat(GamificationLevelCalculator.levelOf(16250)).isEqualTo(9);
        assertThat(GamificationLevelCalculator.levelOf(26249)).isEqualTo(9);
        assertThat(GamificationLevelCalculator.levelOf(26250)).isEqualTo(10);
        assertThat(GamificationLevelCalculator.levelOf(99999)).isEqualTo(10);
    }

    @Test
    void levelOf_normalizes_null_and_negative_xp_to_level_1() {
        assertThat(GamificationLevelCalculator.levelOf(null)).isEqualTo(1);
        assertThat(GamificationLevelCalculator.levelOf(-500)).isEqualTo(1);
    }

    @Test
    void max_level_min_xp_matches_specification() {
        // 명세 GAMIF-002 — Lv.10: 26,250 XP 이상 (만렙)
        assertThat(GamificationLevelCalculator.MAX_LEVEL_MIN_XP).isEqualTo(26250);
        assertThat(GamificationLevelCalculator.MAX_LEVEL).isEqualTo(10);
    }

    @Test
    void current_level_min_xp_returns_band_start_for_each_level() {
        assertThat(GamificationLevelCalculator.currentLevelMinXp(1)).isZero();
        assertThat(GamificationLevelCalculator.currentLevelMinXp(2)).isEqualTo(100);
        assertThat(GamificationLevelCalculator.currentLevelMinXp(3)).isEqualTo(350);
        assertThat(GamificationLevelCalculator.currentLevelMinXp(4)).isEqualTo(850);
        assertThat(GamificationLevelCalculator.currentLevelMinXp(5)).isEqualTo(1750);
        assertThat(GamificationLevelCalculator.currentLevelMinXp(6)).isEqualTo(3250);
        assertThat(GamificationLevelCalculator.currentLevelMinXp(7)).isEqualTo(5750);
        assertThat(GamificationLevelCalculator.currentLevelMinXp(8)).isEqualTo(9750);
        assertThat(GamificationLevelCalculator.currentLevelMinXp(9)).isEqualTo(16250);
        assertThat(GamificationLevelCalculator.currentLevelMinXp(10)).isEqualTo(26250);
    }

    @Test
    void next_level_returns_null_when_max_level() {
        assertThat(GamificationLevelCalculator.nextLevel(10)).isNull();
        assertThat(GamificationLevelCalculator.nextLevelRequiredXp(10)).isNull();
    }

    @Test
    void next_level_required_xp_returns_next_band_start() {
        assertThat(GamificationLevelCalculator.nextLevel(1)).isEqualTo(2);
        assertThat(GamificationLevelCalculator.nextLevelRequiredXp(1)).isEqualTo(100);
        assertThat(GamificationLevelCalculator.nextLevelRequiredXp(9)).isEqualTo(26250);
    }

    @Test
    void progress_pct_clamps_to_0_and_100() {
        // 경계 — 현재 레벨 시작값
        assertThat(GamificationLevelCalculator.progressPct(0, 1)).isZero();
        assertThat(GamificationLevelCalculator.progressPct(100, 2)).isZero();
        // 다음 레벨 진입 직전(현재 레벨 마지막 1XP 전) → 99%
        assertThat(GamificationLevelCalculator.progressPct(99, 1)).isEqualTo(99);
        // 만렙은 항상 100
        assertThat(GamificationLevelCalculator.progressPct(26250, 10)).isEqualTo(100);
        assertThat(GamificationLevelCalculator.progressPct(99999, 10)).isEqualTo(100);
        // 레벨/XP 불일치(데이터 손상) 시 음수 → 0으로 클램프
        assertThat(GamificationLevelCalculator.progressPct(0, 3)).isZero();
    }
}
