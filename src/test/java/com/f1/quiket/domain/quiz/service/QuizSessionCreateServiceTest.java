package com.f1.quiket.domain.quiz.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.f1.quiket.domain.part.entity.Part;
import com.f1.quiket.domain.part.repository.PartRepository;
import com.f1.quiket.domain.quiz.dto.QuizCreateRequest;
import com.f1.quiket.domain.quiz.dto.QuizGenerationAcceptedResponse;
import com.f1.quiket.domain.quiz.entity.QuizSession;
import com.f1.quiket.domain.quiz.entity.QuizSessionScope;
import com.f1.quiket.domain.quiz.repository.QuizSessionRepository;
import com.f1.quiket.domain.quiz.repository.QuizSessionScopeRepository;
import com.f1.quiket.domain.subject.entity.Subject;
import com.f1.quiket.domain.subject.repository.SubjectRepository;
import com.f1.quiket.global.error.CustomException;
import com.f1.quiket.global.response.ErrorCode;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

class QuizSessionCreateServiceTest {

    private SubjectRepository subjectRepository;
    private PartRepository partRepository;
    private QuizSessionRepository quizSessionRepository;
    private QuizSessionScopeRepository quizSessionScopeRepository;
    private QuizSessionCreateService quizSessionCreateService;

    @BeforeEach
    void setUp() {
        subjectRepository = mock(SubjectRepository.class);
        partRepository = mock(PartRepository.class);
        quizSessionRepository = mock(QuizSessionRepository.class);
        quizSessionScopeRepository = mock(QuizSessionScopeRepository.class);
        quizSessionCreateService = new QuizSessionCreateService(
                subjectRepository,
                partRepository,
                quizSessionRepository,
                quizSessionScopeRepository
        );
    }

    @Test
    @SuppressWarnings("unchecked")
    void createQuizSession_saves_session_and_scopes_with_default_options() {
        Long userId = 1L;
        Subject subject = subject(10L, "subject-public-id", userId);
        Part part1 = part(100L, "part-public-1", 1000L, subject.getId(), userId);
        Part part2 = part(101L, "part-public-2", 1001L, subject.getId(), userId);
        QuizCreateRequest request = request(
                subject.getPublicId(),
                List.of(part1.getPublicId(), part2.getPublicId()),
                "multiple_choice",
                null,
                10,
                "one_by_one",
                true,
                "per_question",
                60,
                null
        );

        when(quizSessionRepository.existsByUserIdAndStatusInAndDeletedAtIsNull(eq(userId), any()))
                .thenReturn(false);
        when(subjectRepository.findByPublicIdAndUserIdAndDeletedAtIsNull(subject.getPublicId(), userId))
                .thenReturn(Optional.of(subject));
        when(partRepository.findAllByPublicIdInAndSubjectIdAndUserIdAndDeletedAtIsNull(
                request.getPartIds(),
                subject.getId(),
                userId
        )).thenReturn(List.of(part1, part2));
        when(quizSessionRepository.save(any(QuizSession.class)))
                .thenAnswer(invocation -> {
                    QuizSession quizSession = invocation.getArgument(0);
                    ReflectionTestUtils.setField(quizSession, "id", 500L);
                    return quizSession;
                });

        QuizGenerationAcceptedResponse response = quizSessionCreateService.createQuizSession(userId, request);

        ArgumentCaptor<QuizSession> sessionCaptor = ArgumentCaptor.forClass(QuizSession.class);
        verify(quizSessionRepository).save(sessionCaptor.capture());
        QuizSession savedSession = sessionCaptor.getValue();
        assertThat(savedSession.getPublicId()).isNotBlank();
        assertThat(savedSession.getUserId()).isEqualTo(userId);
        assertThat(savedSession.getSubjectId()).isEqualTo(subject.getId());
        assertThat(savedSession.getQuizType()).isEqualTo("multiple_choice");
        assertThat(savedSession.getChoiceCount()).isEqualTo(4);
        assertThat(savedSession.getQuestionCount()).isEqualTo(10);
        assertThat(savedSession.getPlayMode()).isEqualTo("one_by_one");
        assertThat(savedSession.getTimerEnabled()).isTrue();
        assertThat(savedSession.getTimerScope()).isEqualTo("per_question");
        assertThat(savedSession.getTimerSeconds()).isEqualTo(60);
        assertThat(savedSession.getDifficulty()).isEqualTo("medium");
        assertThat(savedSession.getStatus()).isEqualTo("pending");
        assertThat(savedSession.getJobId()).startsWith("quiz-job-");

        ArgumentCaptor<Iterable<QuizSessionScope>> scopeCaptor = ArgumentCaptor.forClass(Iterable.class);
        verify(quizSessionScopeRepository).saveAll(scopeCaptor.capture());
        List<QuizSessionScope> scopes = toList(scopeCaptor.getValue());
        assertThat(scopes).hasSize(2);
        assertThat(scopes)
                .extracting(QuizSessionScope::getQuizSessionId)
                .containsOnly(500L);
        assertThat(scopes)
                .extracting(QuizSessionScope::getPartId)
                .containsExactly(part1.getId(), part2.getId());

        assertThat(response.getQuizSessionId()).isEqualTo(savedSession.getPublicId());
        assertThat(response.getJobId()).isEqualTo(savedSession.getJobId());
        assertThat(response.getStatus()).isEqualTo("pending");
        assertThat(response.getEstimatedSeconds()).isNull();
    }

    @Test
    void createQuizSession_throws_conflict_when_generation_already_active() {
        Long userId = 1L;
        QuizCreateRequest request = request(
                "subject-public-id",
                List.of("part-public-id"),
                "multiple_choice",
                4,
                10,
                "one_by_one",
                false,
                null,
                null,
                "medium"
        );
        when(quizSessionRepository.existsByUserIdAndStatusInAndDeletedAtIsNull(eq(userId), any()))
                .thenReturn(true);

        assertThatThrownBy(() -> quizSessionCreateService.createQuizSession(userId, request))
                .isInstanceOf(CustomException.class)
                .extracting(exception -> ((CustomException) exception).getErrorCode())
                .isEqualTo(ErrorCode.QUIZ_GENERATION_IN_PROGRESS);

        verifyNoInteractions(subjectRepository, partRepository, quizSessionScopeRepository);
    }

    @Test
    void createQuizSession_throws_invalid_option_when_ox_has_choice_count() {
        Long userId = 1L;
        Subject subject = subject(10L, "subject-public-id", userId);
        QuizCreateRequest request = request(
                subject.getPublicId(),
                List.of("part-public-id"),
                "ox",
                4,
                10,
                "one_by_one",
                false,
                null,
                null,
                "medium"
        );
        when(subjectRepository.findByPublicIdAndUserIdAndDeletedAtIsNull(subject.getPublicId(), userId))
                .thenReturn(Optional.of(subject));

        assertThatThrownBy(() -> quizSessionCreateService.createQuizSession(userId, request))
                .isInstanceOf(CustomException.class)
                .extracting(exception -> ((CustomException) exception).getErrorCode())
                .isEqualTo(ErrorCode.QUIZ_OPTION_INVALID);
    }

    @Test
    void createQuizSession_throws_invalid_option_when_timer_scope_mismatched() {
        Long userId = 1L;
        Subject subject = subject(10L, "subject-public-id", userId);
        QuizCreateRequest request = request(
                subject.getPublicId(),
                List.of("part-public-id"),
                "multiple_choice",
                4,
                10,
                "all_at_once",
                true,
                "per_question",
                60,
                "medium"
        );
        when(subjectRepository.findByPublicIdAndUserIdAndDeletedAtIsNull(subject.getPublicId(), userId))
                .thenReturn(Optional.of(subject));

        assertThatThrownBy(() -> quizSessionCreateService.createQuizSession(userId, request))
                .isInstanceOf(CustomException.class)
                .extracting(exception -> ((CustomException) exception).getErrorCode())
                .isEqualTo(ErrorCode.QUIZ_OPTION_INVALID);
    }

    @Test
    void createQuizSession_throws_invalid_scope_when_part_ids_are_duplicated() {
        Long userId = 1L;
        Subject subject = subject(10L, "subject-public-id", userId);
        QuizCreateRequest request = request(
                subject.getPublicId(),
                List.of("part-public-id", "part-public-id"),
                "multiple_choice",
                4,
                10,
                "one_by_one",
                false,
                null,
                null,
                "medium"
        );
        when(subjectRepository.findByPublicIdAndUserIdAndDeletedAtIsNull(subject.getPublicId(), userId))
                .thenReturn(Optional.of(subject));

        assertThatThrownBy(() -> quizSessionCreateService.createQuizSession(userId, request))
                .isInstanceOf(CustomException.class)
                .extracting(exception -> ((CustomException) exception).getErrorCode())
                .isEqualTo(ErrorCode.QUIZ_SCOPE_INVALID);
    }

    @Test
    void createQuizSession_throws_invalid_scope_when_part_is_not_found() {
        Long userId = 1L;
        Subject subject = subject(10L, "subject-public-id", userId);
        QuizCreateRequest request = request(
                subject.getPublicId(),
                List.of("part-public-1", "part-public-2"),
                "multiple_choice",
                4,
                10,
                "one_by_one",
                false,
                null,
                null,
                "medium"
        );
        when(subjectRepository.findByPublicIdAndUserIdAndDeletedAtIsNull(subject.getPublicId(), userId))
                .thenReturn(Optional.of(subject));
        when(partRepository.findAllByPublicIdInAndSubjectIdAndUserIdAndDeletedAtIsNull(
                request.getPartIds(),
                subject.getId(),
                userId
        )).thenReturn(List.of(part(100L, "part-public-1", 1000L, subject.getId(), userId)));

        assertThatThrownBy(() -> quizSessionCreateService.createQuizSession(userId, request))
                .isInstanceOf(CustomException.class)
                .extracting(exception -> ((CustomException) exception).getErrorCode())
                .isEqualTo(ErrorCode.QUIZ_SCOPE_INVALID);
    }

    private QuizCreateRequest request(
            String subjectId,
            List<String> partIds,
            String quizType,
            Integer choiceCount,
            Integer questionCount,
            String playMode,
            Boolean timerEnabled,
            String timerScope,
            Integer timerSeconds,
            String difficulty
    ) {
        QuizCreateRequest request = org.springframework.beans.BeanUtils.instantiateClass(QuizCreateRequest.class);
        ReflectionTestUtils.setField(request, "subjectId", subjectId);
        ReflectionTestUtils.setField(request, "partIds", partIds);
        ReflectionTestUtils.setField(request, "quizType", quizType);
        ReflectionTestUtils.setField(request, "choiceCount", choiceCount);
        ReflectionTestUtils.setField(request, "questionCount", questionCount);
        ReflectionTestUtils.setField(request, "playMode", playMode);
        ReflectionTestUtils.setField(request, "timerEnabled", timerEnabled);
        ReflectionTestUtils.setField(request, "timerScope", timerScope);
        ReflectionTestUtils.setField(request, "timerSeconds", timerSeconds);
        ReflectionTestUtils.setField(request, "difficulty", difficulty);
        return request;
    }

    private Subject subject(Long id, String publicId, Long userId) {
        Subject subject = newEntity(Subject.class);
        ReflectionTestUtils.setField(subject, "id", id);
        ReflectionTestUtils.setField(subject, "publicId", publicId);
        ReflectionTestUtils.setField(subject, "userId", userId);
        ReflectionTestUtils.setField(subject, "name", "데이터베이스");
        ReflectionTestUtils.setField(subject, "purpose", "exam");
        return subject;
    }

    private Part part(Long id, String publicId, Long chapterId, Long subjectId, Long userId) {
        Part part = newEntity(Part.class);
        ReflectionTestUtils.setField(part, "id", id);
        ReflectionTestUtils.setField(part, "publicId", publicId);
        ReflectionTestUtils.setField(part, "chapterId", chapterId);
        ReflectionTestUtils.setField(part, "subjectId", subjectId);
        ReflectionTestUtils.setField(part, "userId", userId);
        ReflectionTestUtils.setField(part, "name", "데이터 모델링");
        ReflectionTestUtils.setField(part, "partNumber", 1);
        return part;
    }

    private List<QuizSessionScope> toList(Iterable<QuizSessionScope> scopes) {
        return java.util.stream.StreamSupport.stream(scopes.spliterator(), false)
                .toList();
    }

    private <T> T newEntity(Class<T> type) {
        return org.springframework.beans.BeanUtils.instantiateClass(type);
    }
}
