package com.f1.quiket.domain.material.port;

import com.f1.quiket.domain.material.dto.StudyMaterialFile;
import com.f1.quiket.domain.material.dto.StudyMaterialPdfTextExtractionResult;

/**
 * 학습 자료 PDF 텍스트 추출 포트
 *
 * 도메인 계층의 PDF 파서 구현 의존성 분리
 */
public interface StudyMaterialPdfTextExtractor {

    /**
     * PDF 텍스트 추출 및 레이어 판별
     */
    StudyMaterialPdfTextExtractionResult extract(StudyMaterialFile pdfFile);
}

