package com.f1.quiket.domain.quiz.service;

import com.f1.quiket.domain.quiz.dto.QuizResultResponse;
import com.f1.quiket.domain.quiz.dto.QuizReviewItemResponse;
import com.f1.quiket.domain.quiz.entity.Question;
import com.f1.quiket.domain.quiz.entity.QuestionAnswer;
import com.f1.quiket.domain.quiz.entity.QuestionOption;
import com.f1.quiket.domain.quiz.entity.QuizPlayAnswer;
import com.f1.quiket.domain.quiz.entity.QuizPlaySession;
import com.f1.quiket.domain.quiz.entity.QuizResult;
import com.f1.quiket.domain.quiz.entity.QuizSession;
import com.f1.quiket.domain.subject.entity.Subject;
import com.f1.quiket.domain.user.entity.User;
import com.f1.quiket.global.error.CustomException;
import com.f1.quiket.global.response.ErrorCode;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class QuizResultResponseAssembler {

    public QuizResultResponse build(
            QuizResult result,
            QuizPlaySession playSession,
            QuizSession quizSession,
            Subject subject,
            User user,
            List<Question> questions,
            Map<Long, List<QuestionOption>> optionsByQuestionId,
            Map<Long, QuestionAnswer> answersByQuestionId,
            List<QuizPlayAnswer> playAnswers
    ) {
        Map<Long, QuizPlayAnswer> playAnswersByQuestionId = playAnswers.stream()
                .collect(Collectors.toMap(QuizPlayAnswer::getQuestionId, Function.identity()));
        List<QuizReviewItemResponse> reviewItems = questions.stream()
                .map(question -> QuizReviewItemResponse.of(
                        question,
                        optionsByQuestionId.getOrDefault(question.getId(), List.of()),
                        answersByQuestionId.get(question.getId()),
                        getPlayAnswer(playAnswersByQuestionId, question)
                ))
                .toList();
        return QuizResultResponse.of(result, playSession, quizSession, subject, user, reviewItems);
    }

    private QuizPlayAnswer getPlayAnswer(Map<Long, QuizPlayAnswer> playAnswersByQuestionId, Question question) {
        QuizPlayAnswer playAnswer = playAnswersByQuestionId.get(question.getId());
        if (playAnswer == null) {
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR, "퀴즈 풀이 답안 정보가 올바르지 않습니다.");
        }
        return playAnswer;
    }
}
