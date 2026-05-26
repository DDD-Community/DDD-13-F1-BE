package com.f1.quiket.domain.lecture.dto;

import java.util.Arrays;
import lombok.Getter;

/**
 * 파트 분류 방식
 */
@Getter
public enum PartSplitMethod {
    AUTO("auto"),
    MANUAL("manual");

    private final String value;

    PartSplitMethod(String value) {
        this.value = value;
    }

    public static PartSplitMethod from(String value) {
        return Arrays.stream(values())
                .filter(type -> type.value.equalsIgnoreCase(value))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("지원하지 않는 파트 분류 방식입니다."));
    }
}

