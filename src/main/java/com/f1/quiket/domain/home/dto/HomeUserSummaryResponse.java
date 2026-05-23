package com.f1.quiket.domain.home.dto;

import com.f1.quiket.domain.user.entity.User;
import lombok.Builder;
import lombok.Getter;

/**
 * 홈 사용자 요약 응답 DTO
 */
@Getter
@Builder
public class HomeUserSummaryResponse {

    /** 닉네임 */
    private final String nickname;
    /** 도토리 잔고 */
    private final Integer dotoriBalance;
    /** 누적 XP */
    private final Integer xpTotal;
    /** 현재 레벨 */
    private final Integer currentLevel;
    /** 레벨명 */
    private final String levelName;

    /**
     * 사용자 엔티티 기반 생성
     */
    public static HomeUserSummaryResponse from(User user) {
        return HomeUserSummaryResponse.builder()
                .nickname(user.getNickname())
                .dotoriBalance(user.getDotoriBalance())
                .xpTotal(user.getXpTotal())
                .currentLevel(user.getCurrentLevel())
                .levelName(resolveLevelName(user.getCurrentLevel()))
                .build();
    }

    /**
     * 레벨명 산출
     */
    private static String resolveLevelName(Integer currentLevel) {
        // 기본 레벨 구간
        if (currentLevel == null || currentLevel <= 1) {
            return "새싹 다람쥐";
        }
        // 초급 레벨 구간
        if (currentLevel <= 3) {
            return "펜굴리는 다람쥐";
        }
        // 중급 레벨 구간
        if (currentLevel <= 6) {
            return "문제푸는 다람쥐";
        }
        // 고급 레벨 구간
        if (currentLevel <= 9) {
            return "퀴즈장인 다람쥐";
        }
        return "마스터 다람쥐";
    }
}
