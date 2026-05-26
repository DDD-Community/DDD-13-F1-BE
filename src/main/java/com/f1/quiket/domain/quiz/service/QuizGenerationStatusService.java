package com.f1.quiket.domain.quiz.service;

import com.f1.quiket.domain.quiz.dto.QuizGenerationStatusResponse;
import com.f1.quiket.domain.quiz.entity.QuizSession;
import com.f1.quiket.domain.quiz.repository.QuizGenerationJobRepository;
import com.f1.quiket.domain.quiz.repository.QuizSessionRepository;
import com.f1.quiket.global.error.CustomException;
import com.f1.quiket.global.response.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 퀴즈 생성 상태 조회 비즈니스 로직
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class QuizGenerationStatusService {

    private final QuizSessionRepository quizSessionRepository;
    private final QuizGenerationJobRepository quizGenerationJobRepository;

    /**
     * 퀴즈 생성 상태 조회
     */
    public QuizGenerationStatusResponse getGenerationStatus(Long userId, String quizSessionPublicId) {
        QuizSession quizSession = quizSessionRepository
                .findByPublicIdAndUserIdAndDeletedAtIsNull(quizSessionPublicId, userId)
                .orElseThrow(() -> new CustomException(ErrorCode.QUIZ_SESSION_NOT_FOUND));

        return quizGenerationJobRepository.findByQuizSessionId(quizSession.getId())
                .map(generationJob -> QuizGenerationStatusResponse.from(quizSession, generationJob))
                .orElseGet(() -> QuizGenerationStatusResponse.from(quizSession));
    }
}
