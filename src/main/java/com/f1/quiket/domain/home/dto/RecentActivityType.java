package com.f1.quiket.domain.home.dto;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 최근활동 유형
 */
@Getter
@RequiredArgsConstructor
public enum RecentActivityType {

    QUIZ_GENERATING("quiz_generating"),
    QUIZ_READY("quiz_ready"),
    QUIZ_IN_PROGRESS("quiz_in_progress"),
    QUIZ_COMPLETED("quiz_completed"),
    LECTURE_UPLOADED("lecture_uploaded");

    private final String value;

    /**
     * API 응답 값 변환
     */
    @JsonValue
    public String getValue() {
        return value;
    }
}
