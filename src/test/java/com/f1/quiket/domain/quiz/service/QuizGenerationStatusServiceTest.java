package com.f1.quiket.domain.quiz.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.f1.quiket.domain.quiz.dto.QuizGenerationStatusResponse;
import com.f1.quiket.domain.quiz.entity.QuizSession;
import com.f1.quiket.domain.quiz.repository.QuizSessionRepository;
import com.f1.quiket.global.error.CustomException;
import com.f1.quiket.global.response.ErrorCode;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class QuizGenerationStatusServiceTest {

    private QuizSessionRepository quizSessionRepository;
    private QuizGenerationStatusService quizGenerationStatusService;

    @BeforeEach
    void setUp() {
        quizSessionRepository = mock(QuizSessionRepository.class);
        quizGenerationStatusService = new QuizGenerationStatusService(quizSessionRepository);
    }

    @Test
    void getGenerationStatus_returns_pending_status() {
        Long userId = 1L;
        QuizSession quizSession = quizSession("quiz-session-public-id", userId, "pending", "quiz-job-1", null, null);
        when(quizSessionRepository.findByPublicIdAndUserIdAndDeletedAtIsNull(quizSession.getPublicId(), userId))
                .thenReturn(Optional.of(quizSession));

        QuizGenerationStatusResponse response = quizGenerationStatusService.getGenerationStatus(
                userId,
                quizSession.getPublicId()
        );

        assertThat(response.getQuizSessionId()).isEqualTo(quizSession.getPublicId());
        assertThat(response.getJobId()).isEqualTo("quiz-job-1");
        assertThat(response.getStatus()).isEqualTo("pending");
        assertThat(response.getEstimatedSeconds()).isNull();
        assertThat(response.getProgressPct()).isZero();
        assertThat(response.getGeneratedCount()).isNull();
        assertThat(response.getFailReason()).isNull();
    }

    @Test
    void getGenerationStatus_returns_completed_status() {
        Long userId = 1L;
        QuizSession quizSession = quizSession("quiz-session-public-id", userId, "completed", "quiz-job-1", 8, null);
        when(quizSessionRepository.findByPublicIdAndUserIdAndDeletedAtIsNull(quizSession.getPublicId(), userId))
                .thenReturn(Optional.of(quizSession));

        QuizGenerationStatusResponse response = quizGenerationStatusService.getGenerationStatus(
                userId,
                quizSession.getPublicId()
        );

        assertThat(response.getStatus()).isEqualTo("completed");
        assertThat(response.getProgressPct()).isEqualTo(100);
        assertThat(response.getGeneratedCount()).isEqualTo(8);
        assertThat(response.getFailReason()).isNull();
    }

    @Test
    void getGenerationStatus_returns_failed_status() {
        Long userId = 1L;
        QuizSession quizSession = quizSession("quiz-session-public-id", userId, "failed", "quiz-job-1", null, "텍스트 길이 부족");
        when(quizSessionRepository.findByPublicIdAndUserIdAndDeletedAtIsNull(quizSession.getPublicId(), userId))
                .thenReturn(Optional.of(quizSession));

        QuizGenerationStatusResponse response = quizGenerationStatusService.getGenerationStatus(
                userId,
                quizSession.getPublicId()
        );

        assertThat(response.getStatus()).isEqualTo("failed");
        assertThat(response.getProgressPct()).isEqualTo(100);
        assertThat(response.getGeneratedCount()).isNull();
        assertThat(response.getFailReason()).isEqualTo("텍스트 길이 부족");
    }

    @Test
    void getGenerationStatus_throws_not_found_when_quiz_session_missing() {
        Long userId = 1L;
        String quizSessionId = "quiz-session-public-id";
        when(quizSessionRepository.findByPublicIdAndUserIdAndDeletedAtIsNull(quizSessionId, userId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> quizGenerationStatusService.getGenerationStatus(userId, quizSessionId))
                .isInstanceOf(CustomException.class)
                .extracting(exception -> ((CustomException) exception).getErrorCode())
                .isEqualTo(ErrorCode.QUIZ_SESSION_NOT_FOUND);
    }

    private QuizSession quizSession(
            String publicId,
            Long userId,
            String status,
            String jobId,
            Integer generatedCount,
            String failReason
    ) {
        QuizSession quizSession = org.springframework.beans.BeanUtils.instantiateClass(QuizSession.class);
        ReflectionTestUtils.setField(quizSession, "publicId", publicId);
        ReflectionTestUtils.setField(quizSession, "userId", userId);
        ReflectionTestUtils.setField(quizSession, "status", status);
        ReflectionTestUtils.setField(quizSession, "jobId", jobId);
        ReflectionTestUtils.setField(quizSession, "generatedCount", generatedCount);
        ReflectionTestUtils.setField(quizSession, "failReason", failReason);
        return quizSession;
    }
}
