package com.f1.quiket.domain.home.dto;

import lombok.Builder;
import lombok.Getter;

/**
 * 홈 히어로 응답 DTO
 */
@Getter
@Builder
public class HomeHeroResponse {

    /** 활성 퀴즈 존재 여부 */
    private final boolean hasActiveQuiz;
    /** 활성 퀴즈 정보 */
    private final RecentActivityResponse activeQuiz;

    /**
     * 활성 퀴즈 기반 생성
     */
    public static HomeHeroResponse from(RecentActivityResponse activeQuiz) {
        return HomeHeroResponse.builder()
                .hasActiveQuiz(activeQuiz != null)
                .activeQuiz(activeQuiz)
                .build();
    }
}
