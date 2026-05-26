package com.f1.quiket.domain.quiz.dto;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class QuizAiGenerationResponse {

    private List<QuizAiGeneratedQuestion> questions = new ArrayList<>();
}
