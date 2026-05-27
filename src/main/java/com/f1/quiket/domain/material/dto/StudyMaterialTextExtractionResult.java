package com.f1.quiket.domain.material.dto;

import lombok.Builder;
import lombok.Getter;

/**
 * 강의 자료 텍스트 추출 결과 DTO
 */
@Getter
@Builder
public class StudyMaterialTextExtractionResult {
    private final String provider;
    private final String extractedText;
}
