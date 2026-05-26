package com.f1.quiket.domain.lecture.dto;

import java.util.Arrays;
import lombok.Getter;

/**
 * 강의 업로드 타입
 */
@Getter
public enum LectureUploadType {
    PDF("pdf"),
    IMAGE("image"),
    TEXT("text");

    private final String value;

    LectureUploadType(String value) {
        this.value = value;
    }

    public static LectureUploadType from(String value) {
        return Arrays.stream(values())
                .filter(type -> type.value.equalsIgnoreCase(value))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("지원하지 않는 업로드 타입입니다."));
    }
}

