package com.f1.quiket.domain.quiz.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.f1.quiket.domain.quiz.dto.QuizPlaySessionResponse;
import com.f1.quiket.domain.quiz.dto.QuizRetryRequest;
import com.f1.quiket.domain.quiz.entity.Question;
import com.f1.quiket.domain.quiz.entity.QuestionAnswer;
import com.f1.quiket.domain.quiz.entity.QuestionOption;
import com.f1.quiket.domain.quiz.entity.QuizPlayAnswer;
import com.f1.quiket.domain.quiz.entity.QuizPlaySession;
import com.f1.quiket.domain.quiz.entity.QuizResult;
import com.f1.quiket.domain.quiz.entity.QuizSession;
import com.f1.quiket.domain.quiz.entity.QuizSessionScope;
import com.f1.quiket.domain.quiz.repository.QuestionAnswerRepository;
import com.f1.quiket.domain.quiz.repository.QuestionOptionRepository;
import com.f1.quiket.domain.quiz.repository.QuestionRepository;
import com.f1.quiket.domain.quiz.repository.QuizPlayAnswerRepository;
import com.f1.quiket.domain.quiz.repository.QuizPlaySessionRepository;
import com.f1.quiket.domain.quiz.repository.QuizResultRepository;
import com.f1.quiket.domain.quiz.repository.QuizSessionRepository;
import com.f1.quiket.domain.quiz.repository.QuizSessionScopeRepository;
import com.f1.quiket.domain.subject.entity.Subject;
import com.f1.quiket.domain.subject.repository.SubjectRepository;
import com.f1.quiket.global.error.CustomException;
import com.f1.quiket.global.response.ErrorCode;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

class QuizResultRetryWrongServiceTest {

    private QuizResultRepository quizResultRepository;
    private QuizPlaySessionRepository quizPlaySessionRepository;
    private QuizSessionRepository quizSessionRepository;
    private SubjectRepository subjectRepository;
    private QuestionRepository questionRepository;
    private QuestionOptionRepository questionOptionRepository;
    private QuestionAnswerRepository questionAnswerRepository;
    private QuizPlayAnswerRepository quizPlayAnswerRepository;
    private QuizSessionScopeRepository quizSessionScopeRepository;
    private QuizResultRetryWrongService quizResultRetryWrongService;

    @BeforeEach
    void setUp() {
        quizResultRepository = mock(QuizResultRepository.class);
        quizPlaySessionRepository = mock(QuizPlaySessionRepository.class);
        quizSessionRepository = mock(QuizSessionRepository.class);
        subjectRepository = mock(SubjectRepository.class);
        questionRepository = mock(QuestionRepository.class);
        questionOptionRepository = mock(QuestionOptionRepository.class);
        questionAnswerRepository = mock(QuestionAnswerRepository.class);
        quizPlayAnswerRepository = mock(QuizPlayAnswerRepository.class);
        quizSessionScopeRepository = mock(QuizSessionScopeRepository.class);
        quizResultRetryWrongService = new QuizResultRetryWrongService(
                quizResultRepository,
                quizPlaySessionRepository,
                quizSessionRepository,
                subjectRepository,
                questionRepository,
                questionOptionRepository,
                questionAnswerRepository,
                quizPlayAnswerRepository,
                quizSessionScopeRepository
        );
    }

    @Test
    void retryWrong_creates_child_quiz_session_and_retry_wrong_play_session() {
        Long userId = 1L;
        QuizSession parentQuizSession = quizSession(500L, "parent-quiz-session-id", userId, 10L, "completed", 2);
        QuizPlaySession parentPlaySession = firstPlaySession(700L, "parent-play-session-id", parentQuizSession);
        QuizResult result = result(3000L, "result-public-id", parentPlaySession, parentQuizSession, userId);
        Question correctQuestion = question(901L, "correct-question-id", parentQuizSession, 1);
        Question wrongQuestion = question(902L, "wrong-question-id", parentQuizSession, 2);
        QuestionOption correctQuestionOption = option(1001L, correctQuestion.getId(), 1, "맞은 문제 정답", true);
        QuestionOption wrongQuestionCorrectOption = option(1002L, wrongQuestion.getId(), 1, "틀린 문제 정답", true);
        QuestionOption wrongQuestionWrongOption = option(1003L, wrongQuestion.getId(), 2, "틀린 문제 오답", false);
        QuestionAnswer correctAnswer = answer(2001L, correctQuestion.getId(), "1");
        QuestionAnswer wrongAnswer = answer(2002L, wrongQuestion.getId(), "1");
        QuizRetryRequest request = request("retry-wrong-client-session-id", true, null, "seed-1");
        mockBaseData(userId, result, parentPlaySession, parentQuizSession);
        when(quizPlaySessionRepository.findByClientSessionId(request.getClientSessionId()))
                .thenReturn(Optional.empty());
        mockParentQuestionData(
                parentQuizSession,
                userId,
                List.of(correctQuestion, wrongQuestion),
                List.of(
                        playAnswer(parentPlaySession.getId(), correctQuestion.getId(), userId, correctQuestionOption.getId(), true, false),
                        playAnswer(parentPlaySession.getId(), wrongQuestion.getId(), userId, wrongQuestionWrongOption.getId(), false, false)
                ),
                List.of(correctQuestionOption, wrongQuestionCorrectOption, wrongQuestionWrongOption),
                List.of(correctAnswer, wrongAnswer)
        );
        mockChildSaves();

        QuizPlaySessionResponse response = quizResultRetryWrongService.retryWrong(userId, result.getPublicId(), request);

        assertThat(response.getPlaySessionId()).isEqualTo(request.getClientSessionId());
        assertThat(response.getQuizSessionId()).isNotEqualTo(parentQuizSession.getPublicId());
        assertThat(response.getPlayType()).isEqualTo("retry_wrong");
        assertThat(response.getStatus()).isEqualTo("in_progress");

        ArgumentCaptor<QuizSession> quizSessionCaptor = ArgumentCaptor.forClass(QuizSession.class);
        verify(quizSessionRepository).save(quizSessionCaptor.capture());
        QuizSession childQuizSession = quizSessionCaptor.getValue();
        assertThat(childQuizSession.getSubjectId()).isEqualTo(parentQuizSession.getSubjectId());
        assertThat(childQuizSession.getQuestionCount()).isEqualTo(1);
        assertThat(childQuizSession.getStatus()).isEqualTo("completed");
        assertThat(childQuizSession.getGeneratedCount()).isEqualTo(1);
        assertThat(childQuizSession.getPlayMode()).isEqualTo(parentQuizSession.getPlayMode());

        ArgumentCaptor<Iterable<QuizSessionScope>> scopeCaptor = iterableCaptor();
        verify(quizSessionScopeRepository).saveAll(scopeCaptor.capture());
        List<QuizSessionScope> savedScopes = toList(scopeCaptor.getValue());
        assertThat(savedScopes).hasSize(1);
        assertThat(savedScopes.get(0).getQuizSessionId()).isEqualTo(childQuizSession.getId());
        assertThat(savedScopes.get(0).getPartId()).isEqualTo(wrongQuestion.getPartId());

        ArgumentCaptor<Question> questionCaptor = ArgumentCaptor.forClass(Question.class);
        verify(questionRepository).save(questionCaptor.capture());
        Question childQuestion = questionCaptor.getValue();
        assertThat(childQuestion.getQuizSessionId()).isEqualTo(childQuizSession.getId());
        assertThat(childQuestion.getBody()).isEqualTo(wrongQuestion.getBody());
        assertThat(childQuestion.getDisplayOrder()).isEqualTo(1);

        ArgumentCaptor<Iterable<QuestionOption>> optionCaptor = iterableCaptor();
        verify(questionOptionRepository).saveAll(optionCaptor.capture());
        List<QuestionOption> childOptions = toList(optionCaptor.getValue());
        assertThat(childOptions).hasSize(2);
        assertThat(childOptions)
                .extracting(QuestionOption::getQuestionId)
                .containsOnly(childQuestion.getId());

        ArgumentCaptor<QuestionAnswer> answerCaptor = ArgumentCaptor.forClass(QuestionAnswer.class);
        verify(questionAnswerRepository).save(answerCaptor.capture());
        assertThat(answerCaptor.getValue().getQuestionId()).isEqualTo(childQuestion.getId());
        assertThat(answerCaptor.getValue().getAnswerValue()).isEqualTo(wrongAnswer.getAnswerValue());

        ArgumentCaptor<QuizPlaySession> playSessionCaptor = ArgumentCaptor.forClass(QuizPlaySession.class);
        verify(quizPlaySessionRepository).save(playSessionCaptor.capture());
        QuizPlaySession retryWrongPlaySession = playSessionCaptor.getValue();
        assertThat(retryWrongPlaySession.getQuizSessionId()).isEqualTo(childQuizSession.getId());
        assertThat(retryWrongPlaySession.getPlayType()).isEqualTo("retry_wrong");
        assertThat(retryWrongPlaySession.getParentPlaySessionId()).isEqualTo(parentPlaySession.getId());
        assertThat(retryWrongPlaySession.getParentQuizSessionId()).isEqualTo(parentQuizSession.getId());
        assertThat(retryWrongPlaySession.getGeneration()).isEqualTo(1);
        assertThat(retryWrongPlaySession.getQuestionShuffled()).isTrue();
        assertThat(retryWrongPlaySession.getOptionShuffled()).isTrue();
    }

    @Test
    void retryWrong_returns_existing_when_same_client_session_id_is_retried() {
        Long userId = 1L;
        QuizSession parentQuizSession = quizSession(500L, "parent-quiz-session-id", userId, 10L, "completed", 2);
        QuizSession childQuizSession = quizSession(600L, "child-quiz-session-id", userId, 10L, "completed", 1);
        QuizPlaySession parentPlaySession = firstPlaySession(700L, "parent-play-session-id", parentQuizSession);
        QuizPlaySession existing = retryWrongPlaySession(
                701L,
                "retry-wrong-client-session-id",
                childQuizSession,
                parentPlaySession,
                parentQuizSession
        );
        QuizResult result = result(3000L, "result-public-id", parentPlaySession, parentQuizSession, userId);
        QuizRetryRequest request = request(existing.getClientSessionId(), null, null, null);
        mockBaseData(userId, result, parentPlaySession, parentQuizSession);
        when(quizPlaySessionRepository.findByClientSessionId(request.getClientSessionId()))
                .thenReturn(Optional.of(existing));
        when(quizSessionRepository.findByIdAndUserIdAndDeletedAtIsNull(childQuizSession.getId(), userId))
                .thenReturn(Optional.of(childQuizSession));

        QuizPlaySessionResponse response = quizResultRetryWrongService.retryWrong(userId, result.getPublicId(), request);

        assertThat(response.getPlaySessionId()).isEqualTo(existing.getClientSessionId());
        assertThat(response.getQuizSessionId()).isEqualTo(childQuizSession.getPublicId());
        assertThat(response.getPlayType()).isEqualTo("retry_wrong");
        verify(quizSessionRepository, never()).save(any());
        verify(questionRepository, never()).save(any());
    }

    @Test
    void retryWrong_throws_conflict_when_wrong_question_is_empty() {
        Long userId = 1L;
        QuizSession parentQuizSession = quizSession(500L, "parent-quiz-session-id", userId, 10L, "completed", 1);
        QuizPlaySession parentPlaySession = firstPlaySession(700L, "parent-play-session-id", parentQuizSession);
        QuizResult result = result(3000L, "result-public-id", parentPlaySession, parentQuizSession, userId);
        Question question = question(901L, "question-id", parentQuizSession, 1);
        QuestionOption option = option(1001L, question.getId(), 1, "정답", true);
        QuestionAnswer answer = answer(2001L, question.getId(), "1");
        QuizRetryRequest request = request("retry-wrong-client-session-id", null, null, null);
        mockBaseData(userId, result, parentPlaySession, parentQuizSession);
        when(quizPlaySessionRepository.findByClientSessionId(request.getClientSessionId()))
                .thenReturn(Optional.empty());
        mockParentQuestionData(
                parentQuizSession,
                userId,
                List.of(question),
                List.of(playAnswer(parentPlaySession.getId(), question.getId(), userId, option.getId(), true, false)),
                List.of(option),
                List.of(answer)
        );

        assertThatThrownBy(() -> quizResultRetryWrongService.retryWrong(userId, result.getPublicId(), request))
                .isInstanceOf(CustomException.class)
                .extracting(exception -> ((CustomException) exception).getErrorCode())
                .isEqualTo(ErrorCode.CONFLICT);
    }

    @Test
    void retryWrong_throws_conflict_when_client_session_id_belongs_to_other_session() {
        Long userId = 1L;
        QuizSession parentQuizSession = quizSession(500L, "parent-quiz-session-id", userId, 10L, "completed", 1);
        QuizPlaySession parentPlaySession = firstPlaySession(700L, "parent-play-session-id", parentQuizSession);
        QuizPlaySession existing = firstPlaySession(701L, "retry-wrong-client-session-id", parentQuizSession);
        QuizResult result = result(3000L, "result-public-id", parentPlaySession, parentQuizSession, userId);
        QuizRetryRequest request = request(existing.getClientSessionId(), null, null, null);
        mockBaseData(userId, result, parentPlaySession, parentQuizSession);
        when(quizPlaySessionRepository.findByClientSessionId(request.getClientSessionId()))
                .thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> quizResultRetryWrongService.retryWrong(userId, result.getPublicId(), request))
                .isInstanceOf(CustomException.class)
                .extracting(exception -> ((CustomException) exception).getErrorCode())
                .isEqualTo(ErrorCode.CONFLICT);
    }

    private void mockBaseData(
            Long userId,
            QuizResult result,
            QuizPlaySession parentPlaySession,
            QuizSession parentQuizSession
    ) {
        when(quizResultRepository.findByPublicIdAndUserId(result.getPublicId(), userId))
                .thenReturn(Optional.of(result));
        when(quizPlaySessionRepository.findByIdAndUserId(parentPlaySession.getId(), userId))
                .thenReturn(Optional.of(parentPlaySession));
        when(quizSessionRepository.findByIdAndUserIdAndDeletedAtIsNull(parentQuizSession.getId(), userId))
                .thenReturn(Optional.of(parentQuizSession));
        when(subjectRepository.findByIdAndUserIdAndDeletedAtIsNull(parentQuizSession.getSubjectId(), userId))
                .thenReturn(Optional.of(subject(parentQuizSession.getSubjectId(), userId)));
    }

    private void mockParentQuestionData(
            QuizSession parentQuizSession,
            Long userId,
            List<Question> questions,
            List<QuizPlayAnswer> playAnswers,
            List<QuestionOption> options,
            List<QuestionAnswer> answers
    ) {
        List<Long> questionIds = questions.stream().map(Question::getId).toList();
        when(questionRepository.findAllByQuizSessionIdAndUserIdOrderByDisplayOrderAscIdAsc(parentQuizSession.getId(), userId))
                .thenReturn(questions);
        when(quizPlayAnswerRepository.findAllByPlaySessionId(playAnswers.get(0).getPlaySessionId()))
                .thenReturn(playAnswers);
        when(questionOptionRepository.findAllByQuestionIdInOrderByQuestionIdAscOptionNumberAsc(questionIds))
                .thenReturn(options);
        when(questionAnswerRepository.findAllByQuestionIdIn(questionIds))
                .thenReturn(answers);
    }

    private void mockChildSaves() {
        AtomicLong questionId = new AtomicLong(9000L);
        when(quizSessionRepository.save(any(QuizSession.class)))
                .thenAnswer(invocation -> {
                    QuizSession quizSession = invocation.getArgument(0);
                    ReflectionTestUtils.setField(quizSession, "id", 600L);
                    return quizSession;
                });
        when(quizSessionScopeRepository.saveAll(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(questionRepository.save(any(Question.class)))
                .thenAnswer(invocation -> {
                    Question question = invocation.getArgument(0);
                    ReflectionTestUtils.setField(question, "id", questionId.incrementAndGet());
                    return question;
                });
        when(questionOptionRepository.saveAll(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(questionAnswerRepository.save(any(QuestionAnswer.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(quizPlaySessionRepository.save(any(QuizPlaySession.class)))
                .thenAnswer(invocation -> {
                    QuizPlaySession playSession = invocation.getArgument(0);
                    ReflectionTestUtils.setField(playSession, "id", 800L);
                    return playSession;
                });
    }

    private Subject subject(Long id, Long userId) {
        Subject subject = newEntity(Subject.class);
        ReflectionTestUtils.setField(subject, "id", id);
        ReflectionTestUtils.setField(subject, "userId", userId);
        return subject;
    }

    private QuizSession quizSession(
            Long id,
            String publicId,
            Long userId,
            Long subjectId,
            String status,
            Integer questionCount
    ) {
        QuizSession quizSession = QuizSession.create(
                publicId,
                userId,
                subjectId,
                "multiple_choice",
                4,
                questionCount,
                "one_by_one",
                true,
                "per_question",
                60,
                "medium",
                status,
                "quiz-job-id"
        );
        ReflectionTestUtils.setField(quizSession, "id", id);
        return quizSession;
    }

    private QuizPlaySession firstPlaySession(Long id, String clientSessionId, QuizSession quizSession) {
        QuizPlaySession playSession = QuizPlaySession.createFirst(
                clientSessionId,
                quizSession.getId(),
                quizSession.getUserId(),
                quizSession.getSubjectId(),
                false,
                true,
                null
        );
        ReflectionTestUtils.setField(playSession, "id", id);
        return playSession;
    }

    private QuizPlaySession retryWrongPlaySession(
            Long id,
            String clientSessionId,
            QuizSession childQuizSession,
            QuizPlaySession parentPlaySession,
            QuizSession parentQuizSession
    ) {
        QuizPlaySession playSession = QuizPlaySession.createRetryWrong(
                clientSessionId,
                childQuizSession.getId(),
                childQuizSession.getUserId(),
                childQuizSession.getSubjectId(),
                parentPlaySession.getId(),
                parentQuizSession.getId(),
                parentPlaySession.getGeneration() + 1,
                true,
                true,
                null
        );
        ReflectionTestUtils.setField(playSession, "id", id);
        return playSession;
    }

    private QuizResult result(
            Long id,
            String publicId,
            QuizPlaySession playSession,
            QuizSession quizSession,
            Long userId
    ) {
        QuizResult result = QuizResult.create(
                publicId,
                playSession.getId(),
                quizSession.getId(),
                userId,
                quizSession.getSubjectId(),
                2,
                1,
                1,
                0,
                50,
                430000,
                1,
                4,
                false,
                null,
                true,
                false
        );
        ReflectionTestUtils.setField(result, "id", id);
        return result;
    }

    private Question question(Long id, String publicId, QuizSession quizSession, Integer displayOrder) {
        Question question = newEntity(Question.class);
        ReflectionTestUtils.setField(question, "id", id);
        ReflectionTestUtils.setField(question, "publicId", publicId);
        ReflectionTestUtils.setField(question, "quizSessionId", quizSession.getId());
        ReflectionTestUtils.setField(question, "userId", quizSession.getUserId());
        ReflectionTestUtils.setField(question, "subjectId", quizSession.getSubjectId());
        ReflectionTestUtils.setField(question, "chapterId", 100L + displayOrder);
        ReflectionTestUtils.setField(question, "partId", 200L + displayOrder);
        ReflectionTestUtils.setField(question, "questionType", "multiple_choice");
        ReflectionTestUtils.setField(question, "difficulty", "medium");
        ReflectionTestUtils.setField(question, "summary", "핵심 개념 확인");
        ReflectionTestUtils.setField(question, "body", "문제 " + displayOrder);
        ReflectionTestUtils.setField(question, "correctExplanation", "정답 해설");
        ReflectionTestUtils.setField(question, "incorrectExplanation", "오답 해설");
        ReflectionTestUtils.setField(question, "displayOrder", displayOrder);
        return question;
    }

    private QuestionOption option(Long id, Long questionId, Integer optionNumber, String content, Boolean correct) {
        QuestionOption option = newEntity(QuestionOption.class);
        ReflectionTestUtils.setField(option, "id", id);
        ReflectionTestUtils.setField(option, "questionId", questionId);
        ReflectionTestUtils.setField(option, "optionNumber", optionNumber);
        ReflectionTestUtils.setField(option, "content", content);
        ReflectionTestUtils.setField(option, "correct", correct);
        return option;
    }

    private QuestionAnswer answer(Long id, Long questionId, String answerValue) {
        QuestionAnswer answer = newEntity(QuestionAnswer.class);
        ReflectionTestUtils.setField(answer, "id", id);
        ReflectionTestUtils.setField(answer, "questionId", questionId);
        ReflectionTestUtils.setField(answer, "answerValue", answerValue);
        return answer;
    }

    private QuizPlayAnswer playAnswer(
            Long playSessionId,
            Long questionId,
            Long userId,
            Long selectedOptionId,
            Boolean correctServer,
            Boolean skipped
    ) {
        return QuizPlayAnswer.create(
                playSessionId,
                questionId,
                userId,
                selectedOptionId,
                null,
                correctServer,
                correctServer,
                skipped,
                15000,
                false
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

    private <T> T newEntity(Class<T> type) {
        return org.springframework.beans.BeanUtils.instantiateClass(type);
    }

    private <T> List<T> toList(Iterable<T> values) {
        List<T> result = new ArrayList<>();
        values.forEach(result::add);
        return result;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private <T> ArgumentCaptor<Iterable<T>> iterableCaptor() {
        return (ArgumentCaptor) ArgumentCaptor.forClass(Iterable.class);
    }
}
