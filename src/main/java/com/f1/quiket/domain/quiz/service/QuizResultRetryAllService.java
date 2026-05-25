package com.f1.quiket.domain.quiz.service;

import com.f1.quiket.domain.quiz.dto.QuizPlaySessionResponse;
import com.f1.quiket.domain.quiz.dto.QuizRetryRequest;
import com.f1.quiket.domain.quiz.entity.QuizPlaySession;
import com.f1.quiket.domain.quiz.entity.QuizResult;
import com.f1.quiket.domain.quiz.entity.QuizSession;
import com.f1.quiket.domain.quiz.repository.QuizPlaySessionRepository;
import com.f1.quiket.domain.quiz.repository.QuizResultRepository;
import com.f1.quiket.domain.quiz.repository.QuizSessionRepository;
import com.f1.quiket.global.error.CustomException;
import com.f1.quiket.global.response.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class QuizResultRetryAllService {

    private static final String QUIZ_SESSION_STATUS_COMPLETED = "completed";
    private static final String PLAY_TYPE_RETRY_ALL = "retry_all";

    private final QuizResultRepository quizResultRepository;
    private final QuizSessionRepository quizSessionRepository;
    private final QuizPlaySessionRepository quizPlaySessionRepository;

    public QuizPlaySessionResponse retryAll(Long userId, String resultPublicId, QuizRetryRequest request) {
        QuizResult result = quizResultRepository.findByPublicIdAndUserId(resultPublicId, userId)
                .orElseThrow(() -> new CustomException(ErrorCode.QUIZ_RESULT_NOT_FOUND));
        QuizSession quizSession = quizSessionRepository.findByIdAndUserIdAndDeletedAtIsNull(result.getQuizSessionId(), userId)
                .orElseThrow(() -> new CustomException(ErrorCode.QUIZ_SESSION_NOT_FOUND));
        validateRetryable(quizSession);

        return quizPlaySessionRepository.findByClientSessionId(request.getClientSessionId())
                .map(existing -> responseFromExisting(existing, quizSession))
                .orElseGet(() -> createRetryAllPlaySession(userId, quizSession, request));
    }

    private void validateRetryable(QuizSession quizSession) {
        if (!QUIZ_SESSION_STATUS_COMPLETED.equals(quizSession.getStatus())) {
            throw new CustomException(ErrorCode.QUIZ_SESSION_NOT_COMPLETED);
        }
    }

    private QuizPlaySessionResponse responseFromExisting(QuizPlaySession existing, QuizSession quizSession) {
        if (!existing.isSameStartRequest(quizSession.getId(), quizSession.getUserId(), PLAY_TYPE_RETRY_ALL)) {
            throw new CustomException(ErrorCode.CONFLICT, "이미 다른 풀이 세션에서 사용 중인 clientSessionId입니다.");
        }
        return QuizPlaySessionResponse.of(existing, quizSession);
    }

    private QuizPlaySessionResponse createRetryAllPlaySession(
            Long userId,
            QuizSession quizSession,
            QuizRetryRequest request
    ) {
        QuizPlaySession playSession = QuizPlaySession.createRetryAll(
                request.getClientSessionId(),
                quizSession.getId(),
                userId,
                quizSession.getSubjectId(),
                request.getQuestionShuffled(),
                request.getOptionShuffled(),
                request.getShuffleSeed()
        );
        try {
            QuizPlaySession saved = quizPlaySessionRepository.save(playSession);
            return QuizPlaySessionResponse.of(saved, quizSession);
        } catch (DataIntegrityViolationException e) {
            throw new CustomException(
                    ErrorCode.CONFLICT,
                    "이미 다른 풀이 세션에서 사용 중인 clientSessionId입니다.",
                    e
            );
        }
    }
}
