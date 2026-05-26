package com.f1.quiket.domain.quiz.service;

import com.f1.quiket.domain.quiz.dto.QuizPlaySessionResponse;
import com.f1.quiket.domain.quiz.dto.QuizPlayStartRequest;
import com.f1.quiket.domain.quiz.entity.QuizPlaySession;
import com.f1.quiket.domain.quiz.entity.QuizSession;
import com.f1.quiket.domain.quiz.repository.QuizPlaySessionRepository;
import com.f1.quiket.domain.quiz.repository.QuizSessionRepository;
import com.f1.quiket.global.error.CustomException;
import com.f1.quiket.global.response.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
@Transactional
public class QuizPlaySessionStartService {

    private static final String QUIZ_SESSION_STATUS_COMPLETED = "completed";
    private static final String PLAY_TYPE_FIRST = "first";

    private final QuizSessionRepository quizSessionRepository;
    private final QuizPlaySessionRepository quizPlaySessionRepository;

    public QuizPlaySessionResponse start(Long userId, String quizSessionPublicId, QuizPlayStartRequest request) {
        QuizSession quizSession = quizSessionRepository
                .findByPublicIdAndUserIdAndDeletedAtIsNull(quizSessionPublicId, userId)
                .orElseThrow(() -> new CustomException(ErrorCode.QUIZ_SESSION_NOT_FOUND));
        validateStartable(quizSession, request);

        return quizPlaySessionRepository.findByClientSessionId(request.getClientSessionId())
                .map(existing -> responseFromExisting(existing, quizSession, request))
                .orElseGet(() -> createPlaySession(userId, quizSession, request));
    }

    private void validateStartable(QuizSession quizSession, QuizPlayStartRequest request) {
        if (!QUIZ_SESSION_STATUS_COMPLETED.equals(quizSession.getStatus())) {
            throw new CustomException(ErrorCode.QUIZ_SESSION_NOT_COMPLETED);
        }
        if (!PLAY_TYPE_FIRST.equals(request.getPlayType()) || StringUtils.hasText(request.getParentPlaySessionId())) {
            throw new CustomException(ErrorCode.QUIZ_OPTION_INVALID);
        }
    }

    private QuizPlaySessionResponse responseFromExisting(
            QuizPlaySession existing,
            QuizSession quizSession,
            QuizPlayStartRequest request
    ) {
        if (!existing.isSameStartRequest(quizSession.getId(), quizSession.getUserId(), request.getPlayType())) {
            throw new CustomException(ErrorCode.CONFLICT, "이미 다른 풀이 세션에서 사용 중인 clientSessionId입니다.");
        }
        return QuizPlaySessionResponse.of(existing, quizSession);
    }

    private QuizPlaySessionResponse createPlaySession(
            Long userId,
            QuizSession quizSession,
            QuizPlayStartRequest request
    ) {
        QuizPlaySession playSession = QuizPlaySession.createFirst(
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
            // 사전 find 이후 다른 트랜잭션이 같은 clientSessionId로 먼저 insert한 race 케이스.
            // 같은 트랜잭션은 이미 rollback-only로 마킹된 상태라 여기서 재조회하지 않고
            // CONFLICT 응답으로 위임 — 클라이언트가 동일 ID로 재시도하면 사전 find 분기로 idempotent 처리됨.
            throw new CustomException(
                    ErrorCode.CONFLICT,
                    "이미 다른 풀이 세션에서 사용 중인 clientSessionId입니다.",
                    e
            );
        }
    }
}
