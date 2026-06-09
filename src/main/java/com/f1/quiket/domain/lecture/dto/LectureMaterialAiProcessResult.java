package com.f1.quiket.domain.lecture.dto;

import java.util.List;
import lombok.Builder;
import lombok.Getter;

/**
 * 강의 자료 AI 처리 결과 DTO
 *
 * AI 제공자, 추출 텍스트, 파트 초안 목록, AI 생성 챕터명 전달
 */
@Getter
@Builder
public class LectureMaterialAiProcessResult {
    private final String provider;
    private final String extractedText;
    private final List<LecturePartDraft> parts;
    /**
     * AI가 생성한 챕터명
     * 요청 시 chapterName이 null인 경우에만 값이 존재하며, 그 외엔 null
     */
    private final String generatedChapterName;
}

