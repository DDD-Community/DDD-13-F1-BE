package com.f1.quiket.domain.lecture.entity;

import java.util.Arrays;
import lombok.Getter;

/**
 * 강의 업로드 처리 상태
 */
@Getter
public enum LectureUploadStatus {
    PENDING("pending"),
    PROCESSING("processing"),
    COMPLETED("completed"),
    FAILED("failed");

    private final String value;

    LectureUploadStatus(String value) {
        this.value = value;
    }

    /**
     * 문자열 기반 처리 상태 변환
     */
    public static LectureUploadStatus from(String value) {
        return Arrays.stream(values())
                .filter(status -> status.value.equalsIgnoreCase(value))
                .findFirst()
                .orElse(PENDING);
    }
}
