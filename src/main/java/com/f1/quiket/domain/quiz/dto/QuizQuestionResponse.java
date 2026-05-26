package com.f1.quiket.domain.quiz.dto;

import com.f1.quiket.domain.chapter.entity.Chapter;
import com.f1.quiket.domain.part.entity.Part;
import com.f1.quiket.domain.quiz.entity.Question;
import com.f1.quiket.domain.quiz.entity.QuestionAnswer;
import com.f1.quiket.domain.quiz.entity.QuestionOption;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

/**
 * 퀴즈 문항 응답 DTO
 */
@Getter
@Builder
public class QuizQuestionResponse {

    private final String id;
    private final String subjectId;
    private final String chapterId;
    private final String partId;
    private final String partName;
    private final String questionType;
    private final String difficulty;
    private final String summary;
    private final String body;
    private final String correctExplanation;
    private final String incorrectExplanation;
    private final Integer displayOrder;
    private final List<QuestionOptionResponse> options;
    private final QuestionAnswerResponse answer;

    public static QuizQuestionResponse of(
            Question question,
            String subjectPublicId,
            Chapter chapter,
            Part part,
            List<QuestionOption> options,
            QuestionAnswer answer
    ) {
        return QuizQuestionResponse.builder()
                .id(question.getPublicId())
                .subjectId(subjectPublicId)
                .chapterId(chapter.getPublicId())
                .partId(part.getPublicId())
                .partName(part.getName())
                .questionType(question.getQuestionType())
                .difficulty(question.getDifficulty())
                .summary(question.getSummary())
                .body(question.getBody())
                .correctExplanation(question.getCorrectExplanation())
                .incorrectExplanation(question.getIncorrectExplanation())
                .displayOrder(question.getDisplayOrder())
                .options(options.stream()
                        .map(QuestionOptionResponse::from)
                        .toList())
                .answer(QuestionAnswerResponse.from(answer))
                .build();
    }
}
