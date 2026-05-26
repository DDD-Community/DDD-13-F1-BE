package com.f1.quiket.domain.quiz.dto;

import com.f1.quiket.domain.quiz.entity.Question;
import com.f1.quiket.domain.quiz.entity.QuestionAnswer;
import com.f1.quiket.domain.quiz.entity.QuestionOption;
import com.f1.quiket.domain.quiz.entity.QuizPlayAnswer;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class QuizReviewItemResponse {

    private final String questionId;
    private final Integer displayOrder;
    private final String summary;
    private final String body;
    private final List<QuestionOptionResponse> options;
    private final String selectedOptionId;
    private final String selectedValue;
    private final String answerValue;
    private final Boolean correctServer;
    private final Boolean skipped;
    private final String correctExplanation;
    private final String incorrectExplanation;

    public static QuizReviewItemResponse of(
            Question question,
            List<QuestionOption> options,
            QuestionAnswer answer,
            QuizPlayAnswer playAnswer
    ) {
        return QuizReviewItemResponse.builder()
                .questionId(question.getPublicId())
                .displayOrder(question.getDisplayOrder())
                .summary(question.getSummary())
                .body(question.getBody())
                .options(options.stream()
                        .map(QuestionOptionResponse::from)
                        .toList())
                .selectedOptionId(playAnswer.getSelectedOptionId() == null ? null : String.valueOf(playAnswer.getSelectedOptionId()))
                .selectedValue(playAnswer.getSelectedValue())
                .answerValue(answer.getAnswerValue())
                .correctServer(playAnswer.getCorrectServer())
                .skipped(playAnswer.getSkipped())
                .correctExplanation(question.getCorrectExplanation())
                .incorrectExplanation(question.getIncorrectExplanation())
                .build();
    }
}
