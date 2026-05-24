package com.f1.quiket.domain.home.dto;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

/**
 * 과목 요약 응답 DTO
 */
@Getter
@Builder
public class SubjectSummaryResponse {

    /** 과목 식별자 */
    private final String id;
    /** 과목명 */
    private final String name;
    /** 학습 목적 */
    private final String purpose;
    /** 챕터 수 */
    private final Integer chapterCount;
    /** 파트 수 */
    private final Integer partCount;
    /** 마지막 활동 시각 */
    private final LocalDateTime lastActivityAt;
    /** 시험 일정 */
    private final SubjectExamScheduleResponse examSchedule;
}
