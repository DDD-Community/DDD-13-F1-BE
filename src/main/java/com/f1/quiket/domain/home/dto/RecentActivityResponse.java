package com.f1.quiket.domain.home.dto;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

/**
 * 최근활동 응답 DTO
 */
@Getter
@Builder
public class RecentActivityResponse {

    /** 활동 식별자 */
    private final String activityId;
    /** 활동 유형 */
    private final RecentActivityType activityType;
    /** 퀴즈 세션 식별자 */
    private final String quizSessionId;
    /** 풀이 세션 식별자 */
    private final String playSessionId;
    /** 결과 식별자 */
    private final String resultId;
    /** 활동 제목 */
    private final String title;
    /** 과목 식별자 */
    private final String subjectId;
    /** 과목명 */
    private final String subjectName;
    /** 활동 상태 */
    private final String status;
    /** 진행률 */
    private final Integer progressPct;
    /** 점수 문구 */
    private final String scoreText;
    /** 생성 시각 */
    private final LocalDateTime createdAt;
}
