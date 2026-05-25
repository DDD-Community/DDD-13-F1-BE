package com.f1.quiket.domain.quiz.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.f1.quiket.domain.gamification.service.GamificationRewardService;
import com.f1.quiket.domain.gamification.service.QuizRewardResult;
import com.f1.quiket.domain.quiz.dto.QuizAnswerSubmitItem;
import com.f1.quiket.domain.quiz.dto.QuizResultSubmitOutcome;
import com.f1.quiket.domain.quiz.dto.QuizResultSubmitRequest;
import com.f1.quiket.domain.quiz.entity.Question;
import com.f1.quiket.domain.quiz.entity.QuestionAnswer;
import com.f1.quiket.domain.quiz.entity.QuestionOption;
import com.f1.quiket.domain.quiz.entity.QuizPlayAnswer;
import com.f1.quiket.domain.quiz.entity.QuizPlaySession;
import com.f1.quiket.domain.quiz.entity.QuizResult;
import com.f1.quiket.domain.quiz.entity.QuizSession;
import com.f1.quiket.domain.quiz.repository.QuestionAnswerRepository;
import com.f1.quiket.domain.quiz.repository.QuestionOptionRepository;
import com.f1.quiket.domain.quiz.repository.QuestionRepository;
import com.f1.quiket.domain.quiz.repository.QuizPlayAnswerRepository;
import com.f1.quiket.domain.quiz.repository.QuizPlaySessionRepository;
import com.f1.quiket.domain.quiz.repository.QuizResultRepository;
import com.f1.quiket.domain.quiz.repository.QuizSessionRepository;
import com.f1.quiket.domain.subject.entity.Subject;
import com.f1.quiket.domain.subject.repository.SubjectRepository;
import com.f1.quiket.domain.user.entity.User;
import com.f1.quiket.domain.user.repository.UserRepository;
import com.f1.quiket.global.error.CustomException;
import com.f1.quiket.global.response.ErrorCode;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

class QuizResultSubmitServiceTest {

    private QuizSessionRepository quizSessionRepository;
    private SubjectRepository subjectRepository;
    private QuestionRepository questionRepository;
    private QuestionOptionRepository questionOptionRepository;
    private QuestionAnswerRepository questionAnswerRepository;
    private QuizPlaySessionRepository quizPlaySessionRepository;
    private QuizPlayAnswerRepository quizPlayAnswerRepository;
    private QuizResultRepository quizResultRepository;
    private UserRepository userRepository;
    private GamificationRewardService gamificationRewardService;
    private QuizResultSubmitService quizResultSubmitService;

    @BeforeEach
    void setUp() {
        quizSessionRepository = mock(QuizSessionRepository.class);
        subjectRepository = mock(SubjectRepository.class);
        questionRepository = mock(QuestionRepository.class);
        questionOptionRepository = mock(QuestionOptionRepository.class);
        questionAnswerRepository = mock(QuestionAnswerRepository.class);
        quizPlaySessionRepository = mock(QuizPlaySessionRepository.class);
        quizPlayAnswerRepository = mock(QuizPlayAnswerRepository.class);
        quizResultRepository = mock(QuizResultRepository.class);
        userRepository = mock(UserRepository.class);
        gamificationRewardService = mock(GamificationRewardService.class);
        quizResultSubmitService = new QuizResultSubmitService(
                quizSessionRepository,
                subjectRepository,
                questionRepository,
                questionOptionRepository,
                questionAnswerRepository,
                quizPlaySessionRepository,
                quizPlayAnswerRepository,
                quizResultRepository,
                userRepository,
                gamificationRewardService,
                new QuizResultResponseAssembler()
        );
    }

    @Test
    void submit_creates_result_with_server_grading() {
        Long userId = 1L;
        User user = user(userId, 12, 360, 3);
        Subject subject = subject(10L, "subject-public-id", userId, "데이터베이스");
        QuizSession quizSession = quizSession(500L, "quiz-session-public-id", userId, subject.getId(), "completed");
        QuizPlaySession playSession = playSession(700L, "client-session-id", quizSession.getId(), userId, subject.getId());
        Question question = question(900L, "question-public-id", quizSession.getId(), userId, subject.getId(), 1);
        QuestionOption correctOption = option(1000L, question.getId(), 1, "정답", true);
        QuestionOption wrongOption = option(1001L, question.getId(), 2, "오답", false);
        QuestionAnswer answer = answer(2000L, question.getId(), "1");
        QuizResultSubmitRequest request = request(
                playSession.getClientSessionId(),
                quizSession.getPublicId(),
                430000,
                List.of(answerItem(question.getPublicId(), String.valueOf(correctOption.getId()), true, false))
        );
        mockSubmitData(user, subject, quizSession, playSession, List.of(question), List.of(correctOption, wrongOption), List.of(answer));
        when(quizResultRepository.findByPlaySessionId(playSession.getId()))
                .thenReturn(Optional.empty());
        mockReward(user, playSession, quizSession, 1, 1, 4, false, null, 3);
        when(quizResultRepository.save(any(QuizResult.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        QuizResultSubmitOutcome outcome = quizResultSubmitService.submit(userId, request);

        assertThat(outcome.isCreated()).isTrue();
        assertThat(outcome.getResponse().getPlaySessionId()).isEqualTo(playSession.getClientSessionId());
        assertThat(outcome.getResponse().getQuizSessionId()).isEqualTo(quizSession.getPublicId());
        assertThat(outcome.getResponse().getSubjectId()).isEqualTo(subject.getPublicId());
        assertThat(outcome.getResponse().getTotalCount()).isEqualTo(1);
        assertThat(outcome.getResponse().getCorrectCount()).isEqualTo(1);
        assertThat(outcome.getResponse().getWrongCount()).isZero();
        assertThat(outcome.getResponse().getSkipCount()).isZero();
        assertThat(outcome.getResponse().getAccuracyPct()).isEqualTo(100);
        assertThat(outcome.getResponse().getScoreMatched()).isTrue();
        assertThat(outcome.getResponse().getAbuseFlagged()).isFalse();
        assertThat(outcome.getResponse().getRewards().getDotoriEarned()).isEqualTo(1);
        assertThat(outcome.getResponse().getRewards().getXpEarned()).isEqualTo(4);
        assertThat(outcome.getResponse().getRewards().getCurrentDotoriBalance()).isEqualTo(13);
        assertThat(outcome.getResponse().getRewards().getCurrentXpTotal()).isEqualTo(364);
        assertThat(outcome.getResponse().getReviewItems()).hasSize(1);
        assertThat(outcome.getResponse().getReviewItems().get(0).getCorrectServer()).isTrue();
        assertThat(playSession.getStatus()).isEqualTo("submitted");
        assertThat(playSession.getElapsedMs()).isEqualTo(430000);

        ArgumentCaptor<Iterable<QuizPlayAnswer>> answersCaptor = answerIterableCaptor();
        verify(quizPlayAnswerRepository).saveAll(answersCaptor.capture());
        List<QuizPlayAnswer> savedAnswers = toList(answersCaptor.getValue());
        assertThat(savedAnswers).hasSize(1);
        assertThat(savedAnswers.get(0).getQuestionId()).isEqualTo(question.getId());
        assertThat(savedAnswers.get(0).getSelectedOptionId()).isEqualTo(correctOption.getId());
        assertThat(savedAnswers.get(0).getCorrectServer()).isTrue();

        ArgumentCaptor<QuizResult> resultCaptor = ArgumentCaptor.forClass(QuizResult.class);
        verify(quizResultRepository).save(resultCaptor.capture());
        QuizResult savedResult = resultCaptor.getValue();
        assertThat(savedResult.getCorrectCount()).isEqualTo(1);
        assertThat(savedResult.getAccuracyPct()).isEqualTo(100);
        assertThat(savedResult.getScoreMatched()).isTrue();
        assertThat(savedResult.getAbuseFlagged()).isFalse();
        assertThat(savedResult.getDotoriEarned()).isEqualTo(1);
        assertThat(savedResult.getXpEarned()).isEqualTo(4);
        verify(gamificationRewardService).applyQuizReward(user, playSession, quizSession, 1);
    }

    @Test
    void submit_returns_existing_result_when_duplicate_request_is_retried() {
        Long userId = 1L;
        User user = user(userId, 13, 364, 3);
        Subject subject = subject(10L, "subject-public-id", userId, "데이터베이스");
        QuizSession quizSession = quizSession(500L, "quiz-session-public-id", userId, subject.getId(), "completed");
        QuizPlaySession playSession = playSession(700L, "client-session-id", quizSession.getId(), userId, subject.getId());
        Question question = question(900L, "question-public-id", quizSession.getId(), userId, subject.getId(), 1);
        QuestionOption option = option(1000L, question.getId(), 1, "정답", true);
        QuestionAnswer answer = answer(2000L, question.getId(), "1");
        QuizResult existingResult = result(3000L, "result-public-id", playSession, quizSession, subject, 1, 1, 0, 0, 100);
        QuizPlayAnswer existingAnswer = playAnswer(playSession.getId(), question.getId(), userId, option.getId(), true, false);
        QuizResultSubmitRequest request = request(
                playSession.getClientSessionId(),
                quizSession.getPublicId(),
                430000,
                List.of(answerItem(question.getPublicId(), String.valueOf(option.getId()), true, false))
        );
        mockSubmitData(user, subject, quizSession, playSession, List.of(question), List.of(option), List.of(answer));
        when(quizResultRepository.findByPlaySessionId(playSession.getId()))
                .thenReturn(Optional.of(existingResult));
        when(quizPlayAnswerRepository.findAllByPlaySessionId(playSession.getId()))
                .thenReturn(List.of(existingAnswer));

        QuizResultSubmitOutcome outcome = quizResultSubmitService.submit(userId, request);

        assertThat(outcome.isCreated()).isFalse();
        assertThat(outcome.getResponse().getResultId()).isEqualTo("result-public-id");
        assertThat(outcome.getResponse().getCorrectCount()).isEqualTo(1);
        assertThat(outcome.getResponse().getRewards().getCurrentDotoriBalance()).isEqualTo(13);
        verify(quizPlayAnswerRepository, never()).saveAll(any());
        verify(quizResultRepository, never()).save(any());
        verify(gamificationRewardService, never()).applyQuizReward(any(), any(), any(), any());
    }

    @Test
    void submit_flags_score_mismatch_and_abuse_when_client_score_differs() {
        Long userId = 1L;
        User user = user(userId, 12, 360, 3);
        Subject subject = subject(10L, "subject-public-id", userId, "데이터베이스");
        QuizSession quizSession = quizSession(500L, "quiz-session-public-id", userId, subject.getId(), "completed");
        QuizPlaySession playSession = playSession(700L, "client-session-id", quizSession.getId(), userId, subject.getId());
        Question question1 = question(901L, "question-public-id-1", quizSession.getId(), userId, subject.getId(), 1);
        Question question2 = question(902L, "question-public-id-2", quizSession.getId(), userId, subject.getId(), 2);
        QuestionOption q1Correct = option(1001L, question1.getId(), 1, "정답", true);
        QuestionOption q2Wrong = option(1002L, question2.getId(), 2, "오답", false);
        QuestionAnswer answer1 = answer(2001L, question1.getId(), "1");
        QuestionAnswer answer2 = answer(2002L, question2.getId(), "1");
        QuizResultSubmitRequest request = request(
                playSession.getClientSessionId(),
                quizSession.getPublicId(),
                120000,
                List.of(
                        answerItem(question1.getPublicId(), String.valueOf(q1Correct.getId()), true, false),
                        answerItem(question2.getPublicId(), String.valueOf(q2Wrong.getId()), true, false)
                )
        );
        mockSubmitData(
                user,
                subject,
                quizSession,
                playSession,
                List.of(question1, question2),
                List.of(q1Correct, q2Wrong),
                List.of(answer1, answer2)
        );
        when(quizResultRepository.findByPlaySessionId(playSession.getId()))
                .thenReturn(Optional.empty());
        mockReward(user, playSession, quizSession, 1, 1, 4, false, null, 3);
        when(quizResultRepository.save(any(QuizResult.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        QuizResultSubmitOutcome outcome = quizResultSubmitService.submit(userId, request);

        assertThat(outcome.getResponse().getCorrectCount()).isEqualTo(1);
        assertThat(outcome.getResponse().getWrongCount()).isEqualTo(1);
        assertThat(outcome.getResponse().getScoreMatched()).isFalse();
        assertThat(outcome.getResponse().getAbuseFlagged()).isTrue();
    }

    @Test
    void submit_throws_invalid_option_when_answer_question_is_missing() {
        Long userId = 1L;
        User user = user(userId, 12, 360, 3);
        Subject subject = subject(10L, "subject-public-id", userId, "데이터베이스");
        QuizSession quizSession = quizSession(500L, "quiz-session-public-id", userId, subject.getId(), "completed");
        QuizPlaySession playSession = playSession(700L, "client-session-id", quizSession.getId(), userId, subject.getId());
        Question question = question(900L, "question-public-id", quizSession.getId(), userId, subject.getId(), 1);
        QuestionOption option = option(1000L, question.getId(), 1, "정답", true);
        QuestionAnswer answer = answer(2000L, question.getId(), "1");
        QuizResultSubmitRequest request = request(
                playSession.getClientSessionId(),
                quizSession.getPublicId(),
                430000,
                List.of(answerItem("unknown-question-id", String.valueOf(option.getId()), true, false))
        );
        mockSubmitData(user, subject, quizSession, playSession, List.of(question), List.of(option), List.of(answer));
        when(quizResultRepository.findByPlaySessionId(playSession.getId()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> quizResultSubmitService.submit(userId, request))
                .isInstanceOf(CustomException.class)
                .extracting(exception -> ((CustomException) exception).getErrorCode())
                .isEqualTo(ErrorCode.QUIZ_OPTION_INVALID);
    }

    private void mockSubmitData(
            User user,
            Subject subject,
            QuizSession quizSession,
            QuizPlaySession playSession,
            List<Question> questions,
            List<QuestionOption> options,
            List<QuestionAnswer> answers
    ) {
        Long userId = user.getId();
        when(userRepository.findByIdAndDeletedAtIsNull(userId))
                .thenReturn(Optional.of(user));
        when(quizPlaySessionRepository.findByClientSessionId(playSession.getClientSessionId()))
                .thenReturn(Optional.of(playSession));
        when(quizSessionRepository.findByPublicIdAndUserIdAndDeletedAtIsNull(quizSession.getPublicId(), userId))
                .thenReturn(Optional.of(quizSession));
        when(subjectRepository.findByIdAndUserIdAndDeletedAtIsNull(subject.getId(), userId))
                .thenReturn(Optional.of(subject));
        when(questionRepository.findAllByQuizSessionIdAndUserIdOrderByDisplayOrderAscIdAsc(quizSession.getId(), userId))
                .thenReturn(questions);
        when(questionOptionRepository.findAllByQuestionIdInOrderByQuestionIdAscOptionNumberAsc(
                questions.stream().map(Question::getId).toList()
        )).thenReturn(options);
        when(questionAnswerRepository.findAllByQuestionIdIn(questions.stream().map(Question::getId).toList()))
                .thenReturn(answers);
    }

    private QuizResultSubmitRequest request(
            String clientSessionId,
            String quizSessionId,
            Integer elapsedMs,
            List<QuizAnswerSubmitItem> answers
    ) {
        QuizResultSubmitRequest request = newEntity(QuizResultSubmitRequest.class);
        ReflectionTestUtils.setField(request, "clientSessionId", clientSessionId);
        ReflectionTestUtils.setField(request, "quizSessionId", quizSessionId);
        ReflectionTestUtils.setField(request, "playType", "first");
        ReflectionTestUtils.setField(request, "elapsedMs", elapsedMs);
        ReflectionTestUtils.setField(request, "answers", answers);
        return request;
    }

    private QuizAnswerSubmitItem answerItem(String questionId, String selectedOptionId, Boolean correctClient, Boolean skipped) {
        QuizAnswerSubmitItem item = newEntity(QuizAnswerSubmitItem.class);
        ReflectionTestUtils.setField(item, "questionId", questionId);
        ReflectionTestUtils.setField(item, "selectedOptionId", selectedOptionId);
        ReflectionTestUtils.setField(item, "correctClient", correctClient);
        ReflectionTestUtils.setField(item, "skipped", skipped);
        ReflectionTestUtils.setField(item, "answerElapsedMs", 15000);
        ReflectionTestUtils.setField(item, "marked", false);
        return item;
    }

    private void mockReward(
            User user,
            QuizPlaySession playSession,
            QuizSession quizSession,
            Integer correctCount,
            Integer dotoriEarned,
            Integer xpEarned,
            Boolean leveledUp,
            Integer newLevel,
            Integer levelAfter
    ) {
        when(gamificationRewardService.applyQuizReward(user, playSession, quizSession, correctCount))
                .thenAnswer(invocation -> {
                    user.applyQuizReward(dotoriEarned, xpEarned, levelAfter);
                    return new QuizRewardResult(
                            dotoriEarned,
                            xpEarned,
                            leveledUp,
                            newLevel,
                            user.getDotoriBalance(),
                            user.getXpTotal()
                    );
                });
    }

    private User user(Long id, Integer dotoriBalance, Integer xpTotal, Integer currentLevel) {
        User user = User.create("user-public-id", "user@example.com", "도토리");
        ReflectionTestUtils.setField(user, "id", id);
        ReflectionTestUtils.setField(user, "emailVerified", true);
        ReflectionTestUtils.setField(user, "dotoriBalance", dotoriBalance);
        ReflectionTestUtils.setField(user, "xpTotal", xpTotal);
        ReflectionTestUtils.setField(user, "currentLevel", currentLevel);
        return user;
    }

    private Subject subject(Long id, String publicId, Long userId, String name) {
        Subject subject = newEntity(Subject.class);
        ReflectionTestUtils.setField(subject, "id", id);
        ReflectionTestUtils.setField(subject, "publicId", publicId);
        ReflectionTestUtils.setField(subject, "userId", userId);
        ReflectionTestUtils.setField(subject, "name", name);
        ReflectionTestUtils.setField(subject, "purpose", "exam");
        return subject;
    }

    private QuizSession quizSession(Long id, String publicId, Long userId, Long subjectId, String status) {
        QuizSession quizSession = QuizSession.create(
                publicId,
                userId,
                subjectId,
                "multiple_choice",
                4,
                1,
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

    private QuizPlaySession playSession(Long id, String clientSessionId, Long quizSessionId, Long userId, Long subjectId) {
        QuizPlaySession playSession = QuizPlaySession.createFirst(
                clientSessionId,
                quizSessionId,
                userId,
                subjectId,
                false,
                true,
                null
        );
        ReflectionTestUtils.setField(playSession, "id", id);
        return playSession;
    }

    private Question question(Long id, String publicId, Long quizSessionId, Long userId, Long subjectId, Integer displayOrder) {
        Question question = newEntity(Question.class);
        ReflectionTestUtils.setField(question, "id", id);
        ReflectionTestUtils.setField(question, "publicId", publicId);
        ReflectionTestUtils.setField(question, "quizSessionId", quizSessionId);
        ReflectionTestUtils.setField(question, "userId", userId);
        ReflectionTestUtils.setField(question, "subjectId", subjectId);
        ReflectionTestUtils.setField(question, "chapterId", 100L + displayOrder);
        ReflectionTestUtils.setField(question, "partId", 200L + displayOrder);
        ReflectionTestUtils.setField(question, "questionType", "multiple_choice");
        ReflectionTestUtils.setField(question, "difficulty", "medium");
        ReflectionTestUtils.setField(question, "summary", "핵심 개념 확인");
        ReflectionTestUtils.setField(question, "body", "다음 중 올바른 것은?");
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

    private QuizResult result(
            Long id,
            String publicId,
            QuizPlaySession playSession,
            QuizSession quizSession,
            Subject subject,
            Integer totalCount,
            Integer correctCount,
            Integer wrongCount,
            Integer skipCount,
            Integer accuracyPct
    ) {
        QuizResult result = QuizResult.create(
                publicId,
                playSession.getId(),
                quizSession.getId(),
                playSession.getUserId(),
                subject.getId(),
                totalCount,
                correctCount,
                wrongCount,
                skipCount,
                accuracyPct,
                playSession.getElapsedMs(),
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

    private List<QuizPlayAnswer> toList(Iterable<QuizPlayAnswer> answers) {
        List<QuizPlayAnswer> result = new ArrayList<>();
        answers.forEach(result::add);
        return result;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private ArgumentCaptor<Iterable<QuizPlayAnswer>> answerIterableCaptor() {
        return (ArgumentCaptor) ArgumentCaptor.forClass(Iterable.class);
    }

    private <T> T newEntity(Class<T> type) {
        return org.springframework.beans.BeanUtils.instantiateClass(type);
    }
}
