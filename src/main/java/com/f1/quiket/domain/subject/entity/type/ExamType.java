package com.f1.quiket.domain.subject.entity.type;

import java.util.Arrays;

/**
 * 시험 유형
 */
public enum ExamType {

    UNIVERSITY("university"),
    MIDDLE_HIGH("middle_high"),
    CERTIFICATE("certificate"),
    CIVIL_SERVICE("civil_service"),
    LANGUAGE("language"),
    OTHER_EXAM("other_exam");

    private final String value;

    ExamType(String value) {
        this.value = value;
    }

    /**
     * 문자열 enum 검증
     */
    public static boolean contains(String value) {
        return Arrays.stream(values()).anyMatch(type -> type.value.equals(value));
    }
}
