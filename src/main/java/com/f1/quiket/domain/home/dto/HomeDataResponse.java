package com.f1.quiket.domain.home.dto;

import java.util.List;
import lombok.Builder;
import lombok.Getter;

/**
 * 홈 메인 데이터 응답 DTO
 */
@Getter
@Builder
public class HomeDataResponse {

    /** 사용자 요약 */
    private final HomeUserSummaryResponse user;
    /** 홈 히어로 영역 */
    private final HomeHeroResponse hero;
    /** D-Day 카드 목록 */
    private final List<SubjectExamScheduleResponse> dDayCards;
    /** 과목 요약 목록 */
    private final List<SubjectSummaryResponse> subjects;
    /** 최근활동 목록 */
    private final List<RecentActivityResponse> recentActivities;
}
