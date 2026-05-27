package com.f1.quiket.domain.material.dto;

import java.util.Arrays;
import lombok.Getter;

/**
 * 학습 자료 입력 타입
 *
 * PDF, 이미지, 직접 입력 텍스트 업로드 방식 표현
 */
@Getter
public enum StudyMaterialUploadType {
    PDF("pdf"),
    IMAGE("image"),
    TEXT("text");

    private final String value;

    StudyMaterialUploadType(String value) {
        this.value = value;
    }

    /**
     * 문자열 기반 학습 자료 입력 타입 변환
     */
    public static StudyMaterialUploadType from(String value) {
        return Arrays.stream(values())
                .filter(type -> type.value.equalsIgnoreCase(value))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("지원하지 않는 업로드 타입입니다."));
    }
}

