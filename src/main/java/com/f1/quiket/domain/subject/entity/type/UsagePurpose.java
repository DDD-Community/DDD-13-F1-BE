package com.f1.quiket.domain.subject.entity.type;

import java.util.Arrays;

/**
 * 기타 이용 목적
 */
public enum UsagePurpose {

    WORK("work"),
    PERSONAL("personal"),
    HOBBY("hobby"),
    MEMORY("memory"),
    OTHER("other");

    private final String value;

    UsagePurpose(String value) {
        this.value = value;
    }

    /**
     * 문자열 enum 검증
     */
    public static boolean contains(String value) {
        return Arrays.stream(values()).anyMatch(purpose -> purpose.value.equals(value));
    }
}
