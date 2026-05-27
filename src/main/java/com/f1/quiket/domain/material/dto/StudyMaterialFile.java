package com.f1.quiket.domain.material.dto;

import lombok.Builder;
import lombok.Getter;

/**
 * 강의 자료 파일 DTO
 */
@Getter
@Builder
public class StudyMaterialFile {
    private final String fileName;
    private final String contentType;
    private final byte[] bytes;
}

