package com.f1.quiket.domain.quiz.dto;

import com.f1.quiket.domain.quiz.entity.QuizSession;
import lombok.Builder;
import lombok.Getter;

/**
 * 퀴즈 생성 접수 응답 DTO
 */
@Getter
@Builder
public class QuizGenerationAcceptedResponse {

    private final String quizSessionId;
    private final String jobId;
    private final String status;
    private final Integer estimatedSeconds;

    public static QuizGenerationAcceptedResponse from(QuizSession quizSession) {
        return QuizGenerationAcceptedResponse.builder()
                .quizSessionId(quizSession.getPublicId())
                .jobId(quizSession.getJobId())
                .status(quizSession.getStatus())
                .estimatedSeconds(null)
                .build();
    }
}
