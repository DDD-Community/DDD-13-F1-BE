package com.f1.quiket.domain.lecture.dto;

import lombok.Builder;
import lombok.Getter;

/**
 * 강의 자료 파일 DTO
 */
@Getter
@Builder
public class LectureMaterialFile {
    private final String fileName;
    private final String contentType;
    private final byte[] bytes;
}

