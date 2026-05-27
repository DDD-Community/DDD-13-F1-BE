package com.f1.quiket.domain.lecture.dto;

import java.util.List;
import lombok.Builder;
import lombok.Getter;

/**
 * 강의 자료 AI 처리 결과 DTO
 *
 * AI 제공자, 추출 텍스트, 파트 초안 목록 전달
 */
@Getter
@Builder
public class LectureMaterialAiProcessResult {
    private final String provider;
    private final String extractedText;
    private final List<LecturePartDraft> parts;
}

