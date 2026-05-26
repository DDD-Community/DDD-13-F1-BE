package com.f1.quiket.domain.lecture.dto;

import lombok.Builder;
import lombok.Getter;

/**
 * 파트 분류 계획 DTO
 */
@Getter
@Builder
public class LecturePartSplitPlan {
    private final Integer partNumber;
    private final String intendedName;
}

