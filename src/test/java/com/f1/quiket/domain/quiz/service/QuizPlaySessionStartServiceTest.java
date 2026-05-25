package com.f1.quiket.domain.quiz.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.f1.quiket.domain.quiz.dto.QuizPlaySessionResponse;
import com.f1.quiket.domain.quiz.dto.QuizPlayStartRequest;
import com.f1.quiket.domain.quiz.entity.QuizPlaySession;
import com.f1.quiket.domain.quiz.entity.QuizSession;
import com.f1.quiket.domain.quiz.repository.QuizPlaySessionRepository;
import com.f1.quiket.domain.quiz.repository.QuizSessionRepository;
import com.f1.quiket.global.error.CustomException;
import com.f1.quiket.global.response.ErrorCode;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

class QuizPlaySessionStartServiceTest {

    private QuizSessionRepository quizSessionRepository;
    private QuizPlaySessionRepository quizPlaySessionRepository;
    private QuizPlaySessionStartService quizPlaySessionStartService;

    @BeforeEach
    void setUp() {
        quizSessionRepository = mock(QuizSessionRepository.class);
        quizPlaySessionRepository = mock(QuizPlaySessionRepository.class);
        quizPlaySessionStartService = new QuizPlaySessionStartService(
                quizSessionRepository,
                quizPlaySessionRepository
        );
    }

    @Test
    void start_creates_first_play_session() {
        Long userId = 1L;
        QuizSession quizSession = quizSession(500L, "quiz-session-public-id", userId, 10L, "completed");
        QuizPlayStartRequest request = request("client-session-id", "first", null, true, false, "seed-1");
        when(quizSessionRepository.findByPublicIdAndUserIdAndDeletedAtIsNull(quizSession.getPublicId(), userId))
                .thenReturn(Optional.of(quizSession));
        when(quizPlaySessionRepository.findByClientSessionId(request.getClientSessionId()))
                .thenReturn(Optional.empty());
        when(quizPlaySessionRepository.save(any(QuizPlaySession.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        QuizPlaySessionResponse response = quizPlaySessionStartService.start(userId, quizSession.getPublicId(), request);

        ArgumentCaptor<QuizPlaySession> playSessionCaptor = ArgumentCaptor.forClass(QuizPlaySession.class);
        verify(quizPlaySessionRepository).save(playSessionCaptor.capture());
        QuizPlaySession savedPlaySession = playSessionCaptor.getValue();
        assertThat(savedPlaySession.getClientSessionId()).isEqualTo("client-session-id");
        assertThat(savedPlaySession.getQuizSessionId()).isEqualTo(quizSession.getId());
        assertThat(savedPlaySession.getUserId()).isEqualTo(userId);
        assertThat(savedPlaySession.getSubjectId()).isEqualTo(quizSession.getSubjectId());
        assertThat(savedPlaySession.getPlayType()).isEqualTo("first");
        assertThat(savedPlaySession.getStatus()).isEqualTo("in_progress");
        assertThat(savedPlaySession.getLastQuestionIndex()).isZero();
        assertThat(savedPlaySession.getElapsedMs()).isZero();
        assertThat(savedPlaySession.getQuestionShuffled()).isTrue();
        assertThat(savedPlaySession.getOptionShuffled()).isFalse();
        assertThat(savedPlaySession.getShuffleSeed()).isEqualTo("seed-1");

        assertThat(response.getPlaySessionId()).isEqualTo("client-session-id");
        assertThat(response.getClientSessionId()).isEqualTo("client-session-id");
        assertThat(response.getQuizSessionId()).isEqualTo(quizSession.getPublicId());
        assertThat(response.getPlayType()).isEqualTo("first");
        assertThat(response.getStatus()).isEqualTo("in_progress");
    }

    @Test
    void start_returns_existing_when_same_client_session_id_is_retried() {
        Long userId = 1L;
        QuizSession quizSession = quizSession(500L, "quiz-session-public-id", userId, 10L, "completed");
        QuizPlayStartRequest request = request("client-session-id", "first", null, null, null, null);
        QuizPlaySession existing = playSession("client-session-id", quizSession.getId(), userId, quizSession.getSubjectId());
        when(quizSessionRepository.findByPublicIdAndUserIdAndDeletedAtIsNull(quizSession.getPublicId(), userId))
                .thenReturn(Optional.of(quizSession));
        when(quizPlaySessionRepository.findByClientSessionId(request.getClientSessionId()))
                .thenReturn(Optional.of(existing));

        QuizPlaySessionResponse response = quizPlaySessionStartService.start(userId, quizSession.getPublicId(), request);

        assertThat(response.getPlaySessionId()).isEqualTo(existing.getClientSessionId());
        assertThat(response.getQuizSessionId()).isEqualTo(quizSession.getPublicId());
        verify(quizPlaySessionRepository).findByClientSessionId(request.getClientSessionId());
    }

    @Test
    void start_throws_conflict_when_client_session_id_belongs_to_other_session() {
        Long userId = 1L;
        QuizSession quizSession = quizSession(500L, "quiz-session-public-id", userId, 10L, "completed");
        QuizPlayStartRequest request = request("client-session-id", "first", null, null, null, null);
        QuizPlaySession existing = playSession("client-session-id", 999L, userId, quizSession.getSubjectId());
        when(quizSessionRepository.findByPublicIdAndUserIdAndDeletedAtIsNull(quizSession.getPublicId(), userId))
                .thenReturn(Optional.of(quizSession));
        when(quizPlaySessionRepository.findByClientSessionId(request.getClientSessionId()))
                .thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> quizPlaySessionStartService.start(userId, quizSession.getPublicId(), request))
                .isInstanceOf(CustomException.class)
                .extracting(exception -> ((CustomException) exception).getErrorCode())
                .isEqualTo(ErrorCode.CONFLICT);
    }

    @Test
    void start_throws_not_completed_when_quiz_generation_is_not_completed() {
        Long userId = 1L;
        QuizSession quizSession = quizSession(500L, "quiz-session-public-id", userId, 10L, "pending");
        QuizPlayStartRequest request = request("client-session-id", "first", null, null, null, null);
        when(quizSessionRepository.findByPublicIdAndUserIdAndDeletedAtIsNull(quizSession.getPublicId(), userId))
                .thenReturn(Optional.of(quizSession));

        assertThatThrownBy(() -> quizPlaySessionStartService.start(userId, quizSession.getPublicId(), request))
                .isInstanceOf(CustomException.class)
                .extracting(exception -> ((CustomException) exception).getErrorCode())
                .isEqualTo(ErrorCode.QUIZ_SESSION_NOT_COMPLETED);

        verifyNoInteractions(quizPlaySessionRepository);
    }

    @Test
    void start_throws_invalid_option_when_play_type_is_not_first() {
        Long userId = 1L;
        QuizSession quizSession = quizSession(500L, "quiz-session-public-id", userId, 10L, "completed");
        QuizPlayStartRequest request = request("client-session-id", "retry_all", null, null, null, null);
        when(quizSessionRepository.findByPublicIdAndUserIdAndDeletedAtIsNull(quizSession.getPublicId(), userId))
                .thenReturn(Optional.of(quizSession));

        assertThatThrownBy(() -> quizPlaySessionStartService.start(userId, quizSession.getPublicId(), request))
                .isInstanceOf(CustomException.class)
                .extracting(exception -> ((CustomException) exception).getErrorCode())
                .isEqualTo(ErrorCode.QUIZ_OPTION_INVALID);
    }

    private QuizSession quizSession(Long id, String publicId, Long userId, Long subjectId, String status) {
        QuizSession quizSession = QuizSession.create(
                publicId,
                userId,
                subjectId,
                "multiple_choice",
                4,
                10,
                "one_by_one",
                false,
                null,
                null,
                "medium",
                status,
                "quiz-job-id"
        );
        ReflectionTestUtils.setField(quizSession, "id", id);
        return quizSession;
    }

    private QuizPlaySession playSession(String clientSessionId, Long quizSessionId, Long userId, Long subjectId) {
        return QuizPlaySession.createFirst(
                clientSessionId,
                quizSessionId,
                userId,
                subjectId,
                false,
                true,
                null
        );
    }

    private QuizPlayStartRequest request(
            String clientSessionId,
            String playType,
            String parentPlaySessionId,
            Boolean questionShuffled,
            Boolean optionShuffled,
            String shuffleSeed
    ) {
        QuizPlayStartRequest request = new QuizPlayStartRequest();
        ReflectionTestUtils.setField(request, "clientSessionId", clientSessionId);
        ReflectionTestUtils.setField(request, "playType", playType);
        ReflectionTestUtils.setField(request, "parentPlaySessionId", parentPlaySessionId);
        ReflectionTestUtils.setField(request, "questionShuffled", questionShuffled);
        ReflectionTestUtils.setField(request, "optionShuffled", optionShuffled);
        ReflectionTestUtils.setField(request, "shuffleSeed", shuffleSeed);
        return request;
    }
}
