package com.f1.quiket.domain.quiz.dto;

import com.f1.quiket.domain.quiz.entity.QuestionOption;
import lombok.Builder;
import lombok.Getter;

/**
 * 퀴즈 문항 선택지 응답 DTO
 */
@Getter
@Builder
public class QuestionOptionResponse {

    private final String id;
    private final Integer optionNumber;
    private final String content;

    public static QuestionOptionResponse from(QuestionOption option) {
        return QuestionOptionResponse.builder()
                .id(String.valueOf(option.getId()))
                .optionNumber(option.getOptionNumber())
                .content(option.getContent())
                .build();
    }
}
