package com.f1.quiket.domain.material.dto;

import lombok.Builder;
import lombok.Getter;

/**
 * 학습 자료 파일 DTO
 *
 * AI와 PDF 파서에 전달할 파일 메타데이터와 바이트 보관
 */
@Getter
@Builder
public class StudyMaterialFile {
    private final String fileName;
    private final String contentType;
    private final byte[] bytes;
}

