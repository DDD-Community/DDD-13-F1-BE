package com.f1.quiket.domain.lecture.dto;

import lombok.Builder;
import lombok.Getter;

/**
 * 생성 파트 초안 DTO
 *
 * AI 분류 결과의 파트 번호, 이름, 본문 전달
 */
@Getter
@Builder
public class LecturePartDraft {
    private final Integer partNumber;
    private final String name;
    private final String content;
}

