package com.f1.quiket.domain.lecture.dto;

import lombok.Builder;
import lombok.Getter;

/**
 * 파트 분류 계획 DTO
 *
 * 수동 분류 시 파트 번호와 의도한 파트명 전달
 */
@Getter
@Builder
public class LecturePartSplitPlan {
    private final Integer partNumber;
    private final String intendedName;
}

