package com.f1.quiket.domain.subject.entity.type;

import java.util.Arrays;

/**
 * 학습 정도
 */
public enum StudyLevel {

    BEGINNER("beginner"),
    CASUAL("casual"),
    REGULAR("regular"),
    EXPERT("expert");

    private final String value;

    StudyLevel(String value) {
        this.value = value;
    }

    /**
     * 문자열 enum 검증
     */
    public static boolean contains(String value) {
        return Arrays.stream(values()).anyMatch(level -> level.value.equals(value));
    }
}
