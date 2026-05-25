package com.f1.quiket.domain.quiz.dto;

import com.f1.quiket.domain.quiz.entity.QuizPlaySession;
import com.f1.quiket.domain.quiz.entity.QuizSession;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class QuizPlaySessionResponse {

    private final String playSessionId;
    private final String clientSessionId;
    private final String quizSessionId;
    private final String playType;
    private final String status;

    public static QuizPlaySessionResponse of(QuizPlaySession playSession, QuizSession quizSession) {
        return QuizPlaySessionResponse.builder()
                .playSessionId(playSession.getClientSessionId())
                .clientSessionId(playSession.getClientSessionId())
                .quizSessionId(quizSession.getPublicId())
                .playType(playSession.getPlayType())
                .status(playSession.getStatus())
                .build();
    }
}
