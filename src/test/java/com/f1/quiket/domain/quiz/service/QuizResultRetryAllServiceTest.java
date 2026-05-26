package com.f1.quiket.domain.quiz.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.f1.quiket.domain.quiz.dto.QuizPlaySessionResponse;
import com.f1.quiket.domain.quiz.dto.QuizRetryRequest;
import com.f1.quiket.domain.quiz.entity.QuizPlaySession;
import com.f1.quiket.domain.quiz.entity.QuizResult;
import com.f1.quiket.domain.quiz.entity.QuizSession;
import com.f1.quiket.domain.quiz.repository.QuizPlaySessionRepository;
import com.f1.quiket.domain.quiz.repository.QuizResultRepository;
import com.f1.quiket.domain.quiz.repository.QuizSessionRepository;
import com.f1.quiket.domain.subject.entity.Subject;
import com.f1.quiket.domain.subject.repository.SubjectRepository;
import com.f1.quiket.global.error.CustomException;
import com.f1.quiket.global.response.ErrorCode;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

class QuizResultRetryAllServiceTest {

    private QuizResultRepository quizResultRepository;
    private QuizSessionRepository quizSessionRepository;
    private QuizPlaySessionRepository quizPlaySessionRepository;
    private SubjectRepository subjectRepository;
    private QuizResultRetryAllService quizResultRetryAllService;

    @BeforeEach
    void setUp() {
        quizResultRepository = mock(QuizResultRepository.class);
        quizSessionRepository = mock(QuizSessionRepository.class);
        quizPlaySessionRepository = mock(QuizPlaySessionRepository.class);
        subjectRepository = mock(SubjectRepository.class);
        quizResultRetryAllService = new QuizResultRetryAllService(
                quizResultRepository,
                quizSessionRepository,
                quizPlaySessionRepository,
                subjectRepository
        );
    }

    @Test
    void retryAll_creates_retry_all_play_session() {
        Long userId = 1L;
        QuizSession quizSession = quizSession(500L, "quiz-session-public-id", userId, 10L, "completed");
        QuizResult result = result(3000L, "result-public-id", quizSession, userId, 700L);
        QuizRetryRequest request = request("retry-client-session-id", true, false, "seed-1");
        when(quizResultRepository.findByPublicIdAndUserId(result.getPublicId(), userId))
                .thenReturn(Optional.of(result));
        when(quizSessionRepository.findByIdAndUserIdAndDeletedAtIsNull(quizSession.getId(), userId))
                .thenReturn(Optional.of(quizSession));
        when(subjectRepository.findByIdAndUserIdAndDeletedAtIsNull(quizSession.getSubjectId(), userId))
                .thenReturn(Optional.of(subject(quizSession.getSubjectId(), userId)));
        when(quizPlaySessionRepository.findByClientSessionId(request.getClientSessionId()))
                .thenReturn(Optional.empty());
        when(quizPlaySessionRepository.save(any(QuizPlaySession.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        QuizPlaySessionResponse response = quizResultRetryAllService.retryAll(userId, result.getPublicId(), request);

        ArgumentCaptor<QuizPlaySession> playSessionCaptor = ArgumentCaptor.forClass(QuizPlaySession.class);
        verify(quizPlaySessionRepository).save(playSessionCaptor.capture());
        QuizPlaySession savedPlaySession = playSessionCaptor.getValue();
        assertThat(savedPlaySession.getClientSessionId()).isEqualTo("retry-client-session-id");
        assertThat(savedPlaySession.getQuizSessionId()).isEqualTo(quizSession.getId());
        assertThat(savedPlaySession.getUserId()).isEqualTo(userId);
        assertThat(savedPlaySession.getSubjectId()).isEqualTo(quizSession.getSubjectId());
        assertThat(savedPlaySession.getPlayType()).isEqualTo("retry_all");
        assertThat(savedPlaySession.getStatus()).isEqualTo("in_progress");
        assertThat(savedPlaySession.getQuestionShuffled()).isTrue();
        assertThat(savedPlaySession.getOptionShuffled()).isFalse();
        assertThat(savedPlaySession.getShuffleSeed()).isEqualTo("seed-1");

        assertThat(response.getPlaySessionId()).isEqualTo("retry-client-session-id");
        assertThat(response.getQuizSessionId()).isEqualTo(quizSession.getPublicId());
        assertThat(response.getPlayType()).isEqualTo("retry_all");
        assertThat(response.getStatus()).isEqualTo("in_progress");
    }

    @Test
    void retryAll_returns_existing_when_same_client_session_id_is_retried() {
        Long userId = 1L;
        QuizSession quizSession = quizSession(500L, "quiz-session-public-id", userId, 10L, "completed");
        QuizResult result = result(3000L, "result-public-id", quizSession, userId, 700L);
        QuizRetryRequest request = request("retry-client-session-id", null, null, null);
        QuizPlaySession existing = retryAllPlaySession("retry-client-session-id", quizSession.getId(), userId, quizSession.getSubjectId());
        when(quizResultRepository.findByPublicIdAndUserId(result.getPublicId(), userId))
                .thenReturn(Optional.of(result));
        when(quizSessionRepository.findByIdAndUserIdAndDeletedAtIsNull(quizSession.getId(), userId))
                .thenReturn(Optional.of(quizSession));
        when(subjectRepository.findByIdAndUserIdAndDeletedAtIsNull(quizSession.getSubjectId(), userId))
                .thenReturn(Optional.of(subject(quizSession.getSubjectId(), userId)));
        when(quizPlaySessionRepository.findByClientSessionId(request.getClientSessionId()))
                .thenReturn(Optional.of(existing));

        QuizPlaySessionResponse response = quizResultRetryAllService.retryAll(userId, result.getPublicId(), request);

        assertThat(response.getPlaySessionId()).isEqualTo(existing.getClientSessionId());
        assertThat(response.getQuizSessionId()).isEqualTo(quizSession.getPublicId());
        assertThat(response.getPlayType()).isEqualTo("retry_all");
    }

    @Test
    void retryAll_throws_not_found_when_result_missing() {
        Long userId = 1L;
        QuizRetryRequest request = request("retry-client-session-id", null, null, null);
        when(quizResultRepository.findByPublicIdAndUserId("missing-result-id", userId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> quizResultRetryAllService.retryAll(userId, "missing-result-id", request))
                .isInstanceOf(CustomException.class)
                .extracting(exception -> ((CustomException) exception).getErrorCode())
                .isEqualTo(ErrorCode.QUIZ_RESULT_NOT_FOUND);
    }

    @Test
    void retryAll_throws_conflict_when_client_session_id_belongs_to_other_session() {
        Long userId = 1L;
        QuizSession quizSession = quizSession(500L, "quiz-session-public-id", userId, 10L, "completed");
        QuizResult result = result(3000L, "result-public-id", quizSession, userId, 700L);
        QuizRetryRequest request = request("retry-client-session-id", null, null, null);
        QuizPlaySession existing = QuizPlaySession.createFirst(
                "retry-client-session-id",
                quizSession.getId(),
                userId,
                quizSession.getSubjectId(),
                false,
                true,
                null
        );
        when(quizResultRepository.findByPublicIdAndUserId(result.getPublicId(), userId))
                .thenReturn(Optional.of(result));
        when(quizSessionRepository.findByIdAndUserIdAndDeletedAtIsNull(quizSession.getId(), userId))
                .thenReturn(Optional.of(quizSession));
        when(subjectRepository.findByIdAndUserIdAndDeletedAtIsNull(quizSession.getSubjectId(), userId))
                .thenReturn(Optional.of(subject(quizSession.getSubjectId(), userId)));
        when(quizPlaySessionRepository.findByClientSessionId(request.getClientSessionId()))
                .thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> quizResultRetryAllService.retryAll(userId, result.getPublicId(), request))
                .isInstanceOf(CustomException.class)
                .extracting(exception -> ((CustomException) exception).getErrorCode())
                .isEqualTo(ErrorCode.CONFLICT);
    }

    @Test
    void retryAll_throws_subject_not_found_when_subject_is_deleted() {
        Long userId = 1L;
        QuizSession quizSession = quizSession(500L, "quiz-session-public-id", userId, 10L, "completed");
        QuizResult result = result(3000L, "result-public-id", quizSession, userId, 700L);
        QuizRetryRequest request = request("retry-client-session-id", null, null, null);
        when(quizResultRepository.findByPublicIdAndUserId(result.getPublicId(), userId))
                .thenReturn(Optional.of(result));
        when(quizSessionRepository.findByIdAndUserIdAndDeletedAtIsNull(quizSession.getId(), userId))
                .thenReturn(Optional.of(quizSession));
        when(subjectRepository.findByIdAndUserIdAndDeletedAtIsNull(quizSession.getSubjectId(), userId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> quizResultRetryAllService.retryAll(userId, result.getPublicId(), request))
                .isInstanceOf(CustomException.class)
                .extracting(exception -> ((CustomException) exception).getErrorCode())
                .isEqualTo(ErrorCode.SUBJECT_NOT_FOUND);
    }

    private Subject subject(Long id, Long userId) {
        Subject subject = org.springframework.beans.BeanUtils.instantiateClass(Subject.class);
        ReflectionTestUtils.setField(subject, "id", id);
        ReflectionTestUtils.setField(subject, "userId", userId);
        return subject;
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

    private QuizResult result(Long id, String publicId, QuizSession quizSession, Long userId, Long playSessionId) {
        QuizResult result = QuizResult.create(
                publicId,
                playSessionId,
                quizSession.getId(),
                userId,
                quizSession.getSubjectId(),
                10,
                8,
                2,
                0,
                80,
                430000,
                8,
                32,
                false,
                null,
                true,
                false
        );
        ReflectionTestUtils.setField(result, "id", id);
        return result;
    }

    private QuizPlaySession retryAllPlaySession(String clientSessionId, Long quizSessionId, Long userId, Long subjectId) {
        return QuizPlaySession.createRetryAll(
                clientSessionId,
                quizSessionId,
                userId,
                subjectId,
                false,
                true,
                null
        );
    }

    private QuizRetryRequest request(
            String clientSessionId,
            Boolean questionShuffled,
            Boolean optionShuffled,
            String shuffleSeed
    ) {
        QuizRetryRequest request = new QuizRetryRequest();
        ReflectionTestUtils.setField(request, "clientSessionId", clientSessionId);
        ReflectionTestUtils.setField(request, "questionShuffled", questionShuffled);
        ReflectionTestUtils.setField(request, "optionShuffled", optionShuffled);
        ReflectionTestUtils.setField(request, "shuffleSeed", shuffleSeed);
        return request;
    }
}
