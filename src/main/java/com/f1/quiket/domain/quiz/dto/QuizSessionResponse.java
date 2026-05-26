package com.f1.quiket.domain.quiz.dto;

import com.f1.quiket.domain.chapter.entity.Chapter;
import com.f1.quiket.domain.part.entity.Part;
import com.f1.quiket.domain.quiz.entity.Question;
import com.f1.quiket.domain.quiz.entity.QuestionAnswer;
import com.f1.quiket.domain.quiz.entity.QuestionOption;
import com.f1.quiket.domain.quiz.entity.QuizSession;
import com.f1.quiket.domain.subject.entity.Subject;
import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Getter;

/**
 * 퀴즈 세트 응답 DTO
 */
@Getter
@Builder
public class QuizSessionResponse {

    private final String id;
    private final String subjectId;
    private final String subjectName;
    private final String quizType;
    private final Integer choiceCount;
    private final Integer questionCount;
    private final String playMode;
    private final Boolean timerEnabled;
    private final String timerScope;
    private final Integer timerSeconds;
    private final String difficulty;
    private final String status;
    private final List<QuizQuestionResponse> questions;

    public static QuizSessionResponse of(
            QuizSession quizSession,
            Subject subject,
            List<Question> questions,
            Map<Long, List<QuestionOption>> optionsByQuestionId,
            Map<Long, QuestionAnswer> answersByQuestionId,
            Map<Long, Chapter> chaptersById,
            Map<Long, Part> partsById
    ) {
        return QuizSessionResponse.builder()
                .id(quizSession.getPublicId())
                .subjectId(subject.getPublicId())
                .subjectName(subject.getName())
                .quizType(quizSession.getQuizType())
                .choiceCount(quizSession.getChoiceCount())
                .questionCount(quizSession.getQuestionCount())
                .playMode(quizSession.getPlayMode())
                .timerEnabled(quizSession.getTimerEnabled())
                .timerScope(quizSession.getTimerScope())
                .timerSeconds(quizSession.getTimerSeconds())
                .difficulty(quizSession.getDifficulty())
                .status(quizSession.getStatus())
                .questions(questions.stream()
                        .map(question -> QuizQuestionResponse.of(
                                question,
                                subject.getPublicId(),
                                chaptersById.get(question.getChapterId()),
                                partsById.get(question.getPartId()),
                                optionsByQuestionId.getOrDefault(question.getId(), List.of()),
                                answersByQuestionId.get(question.getId())
                        ))
                        .toList())
                .build();
    }
}
