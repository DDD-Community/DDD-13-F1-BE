package com.f1.quiket.domain.quiz.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class QuizRetryAvailableResponse {

    private final Boolean retryAll;
    private final Boolean retryWrong;
    private final Integer wrongCount;

    public static QuizRetryAvailableResponse from(Integer wrongCount) {
        return QuizRetryAvailableResponse.builder()
                .retryAll(true)
                .retryWrong(wrongCount > 0)
                .wrongCount(wrongCount)
                .build();
    }
}
