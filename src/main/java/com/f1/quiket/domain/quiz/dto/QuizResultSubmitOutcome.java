package com.f1.quiket.domain.quiz.dto;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class QuizResultSubmitOutcome {

    private final QuizResultResponse response;
    private final boolean created;
}
