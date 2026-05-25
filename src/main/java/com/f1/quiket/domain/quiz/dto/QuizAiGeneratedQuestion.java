package com.f1.quiket.domain.quiz.dto;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class QuizAiGeneratedQuestion {

    private String partId;
    private String questionType;
    private String difficulty;
    private String summary;
    private String body;
    private List<QuizAiGeneratedOption> options = new ArrayList<>();
    private String answerValue;
    private String correctExplanation;
    private String incorrectExplanation;
}
