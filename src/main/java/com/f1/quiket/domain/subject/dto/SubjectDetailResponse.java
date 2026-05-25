package com.f1.quiket.domain.subject.dto;

import java.time.LocalDateTime;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

/**
 * 과목 상세 응답 DTO
 */
@Getter
@Builder
public class SubjectDetailResponse {

    /** 과목 식별자 */
    private final String id;
    /** 과목명 */
    private final String name;
    /** 학습 목적 */
    private final String purpose;
    /** 목적별 상세 */
    private final SubjectPurposeDetailResponse detail;
    /** 생성 시각 */
    private final LocalDateTime createdAt;
    /** 시험 일정 */
    private final SubjectExamScheduleResponse examSchedule;
    /** 챕터 목록 */
    private final List<ChapterWithPartsResponse> chapters;
}
