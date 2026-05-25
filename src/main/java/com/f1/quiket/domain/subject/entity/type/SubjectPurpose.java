package com.f1.quiket.domain.subject.entity.type;

import java.util.Arrays;

/**
 * 과목 학습 목적
 */
public enum SubjectPurpose {

    EXAM("exam"),
    REVIEW("review"),
    OTHER("other");

    private final String value;

    SubjectPurpose(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }

    /**
     * 문자열 enum 변환
     */
    public static SubjectPurpose from(String value) {
        return Arrays.stream(values())
                .filter(purpose -> purpose.value.equals(value))
                .findFirst()
                .orElseThrow(IllegalArgumentException::new);
    }
}
