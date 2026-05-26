package com.f1.quiket.domain.lecture.dto;

import lombok.Builder;
import lombok.Getter;

/**
 * 생성 파트 초안 DTO
 */
@Getter
@Builder
public class LecturePartDraft {
    private final Integer partNumber;
    private final String name;
    private final String content;
}

