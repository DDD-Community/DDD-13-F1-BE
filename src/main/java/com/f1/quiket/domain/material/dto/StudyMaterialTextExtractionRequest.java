package com.f1.quiket.domain.material.dto;

import java.util.List;
import lombok.Builder;
import lombok.Getter;

/**
 * 학습 자료 텍스트 추출 요청 DTO
 *
 * 업로드 타입별 텍스트 원문 또는 파일 목록 전달
 */
@Getter
@Builder
public class StudyMaterialTextExtractionRequest {
    private final StudyMaterialUploadType uploadType;
    private final String text;
    private final List<StudyMaterialFile> files;
}
