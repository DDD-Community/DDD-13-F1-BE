package com.f1.quiket.domain.material.dto;

import lombok.Builder;
import lombok.Getter;

/**
 * 학습 자료 텍스트 추출 결과 DTO
 *
 * 추출 제공자와 추출 텍스트 전달
 */
@Getter
@Builder
public class StudyMaterialTextExtractionResult {
    private final String provider;
    private final String extractedText;
}
