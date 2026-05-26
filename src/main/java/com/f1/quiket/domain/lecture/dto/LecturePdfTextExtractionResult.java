package com.f1.quiket.domain.lecture.dto;

import lombok.Builder;
import lombok.Getter;

/**
 * PDF 텍스트 추출 결과 DTO
 */
@Getter
@Builder
public class LecturePdfTextExtractionResult {
    private final boolean hasTextLayer;
    private final String extractedText;
}

