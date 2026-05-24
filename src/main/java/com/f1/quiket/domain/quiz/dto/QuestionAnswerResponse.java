package com.f1.quiket.domain.quiz.dto;

import com.f1.quiket.domain.quiz.entity.QuestionAnswer;
import lombok.Builder;
import lombok.Getter;

/**
 * 퀴즈 문항 정답 응답 DTO
 */
@Getter
@Builder
public class QuestionAnswerResponse {

    private final String answerValue;

    public static QuestionAnswerResponse from(QuestionAnswer answer) {
        return QuestionAnswerResponse.builder()
                .answerValue(answer.getAnswerValue())
                .build();
    }
}
