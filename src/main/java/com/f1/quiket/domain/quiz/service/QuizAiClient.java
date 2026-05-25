package com.f1.quiket.domain.quiz.service;

import com.f1.quiket.domain.quiz.dto.QuizAiGenerationPrompt;
import com.f1.quiket.domain.quiz.dto.QuizAiGenerationResponse;

public interface QuizAiClient {

    QuizAiGenerationResponse generate(QuizAiGenerationPrompt prompt);
}
