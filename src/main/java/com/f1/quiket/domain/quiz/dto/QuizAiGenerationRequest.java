package com.f1.quiket.domain.quiz.dto;

import com.f1.quiket.domain.part.entity.Part;
import com.f1.quiket.domain.subject.entity.Subject;
import java.util.List;

public record QuizAiGenerationRequest(
        Subject subject,
        List<Part> parts,
        String quizType,
        Integer choiceCount,
        Integer questionCount,
        String playMode,
        Boolean timerEnabled,
        String timerScope,
        Integer timerSeconds,
        String difficulty
) {
}
