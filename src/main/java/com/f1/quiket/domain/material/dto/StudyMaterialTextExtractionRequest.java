package com.f1.quiket.domain.material.dto;

import java.util.List;
import lombok.Builder;
import lombok.Getter;

/**
 * 강의 자료 텍스트 추출 요청 DTO
 */
@Getter
@Builder
public class StudyMaterialTextExtractionRequest {
    private final StudyMaterialUploadType uploadType;
    private final String text;
    private final List<StudyMaterialFile> files;
}
