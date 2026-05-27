package com.f1.quiket.domain.material.dto;

import lombok.Builder;
import lombok.Getter;

/**
 * PDF 텍스트 추출 결과 DTO
 *
 * 텍스트 레이어 존재 여부와 추출 텍스트 전달
 */
@Getter
@Builder
public class StudyMaterialPdfTextExtractionResult {
    private final boolean hasTextLayer;
    private final String extractedText;
}

