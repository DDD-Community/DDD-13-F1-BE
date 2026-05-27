package com.f1.quiket.domain.material.port;

import com.f1.quiket.domain.material.dto.StudyMaterialFile;
import java.util.List;

/**
 * 학습 자료 AI 처리 포트
 *
 * 도메인 계층의 AI 벤더 의존성 분리
 */
public interface StudyMaterialAiGateway {

    /**
     * 이미지 기반 AI 텍스트 생성
     */
    String generateFromImages(String systemMessage, String userMessage, List<StudyMaterialFile> imageFiles);

    /**
     * PDF 기반 AI 텍스트 생성
     */
    String generateFromPdf(String systemMessage, String userMessage, StudyMaterialFile pdfFile);

    /**
     * 텍스트 기반 AI 텍스트 생성
     */
    String generateFromText(String systemMessage, String userMessage);
}

