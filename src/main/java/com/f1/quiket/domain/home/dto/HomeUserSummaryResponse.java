package com.f1.quiket.domain.home.dto;

import com.f1.quiket.domain.user.entity.User;
import com.f1.quiket.domain.user.entity.type.UserLevel;
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
                .levelName(UserLevel.titleOf(user.getCurrentLevel()))
                .build();
    }
}
