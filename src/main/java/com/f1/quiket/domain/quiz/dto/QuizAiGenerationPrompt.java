package com.f1.quiket.domain.quiz.dto;

public record QuizAiGenerationPrompt(
        String systemMessage,
        String userMessage
) {
}
