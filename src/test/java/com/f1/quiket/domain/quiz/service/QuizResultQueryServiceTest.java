package com.f1.quiket.domain.quiz.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.f1.quiket.domain.quiz.dto.QuizResultResponse;
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
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class QuizResultQueryServiceTest {

    private QuizResultRepository quizResultRepository;
    private QuizPlaySessionRepository quizPlaySessionRepository;
    private QuizSessionRepository quizSessionRepository;
    private SubjectRepository subjectRepository;
    private UserRepository userRepository;
    private QuestionRepository questionRepository;
    private QuestionOptionRepository questionOptionRepository;
    private QuestionAnswerRepository questionAnswerRepository;
    private QuizPlayAnswerRepository quizPlayAnswerRepository;
    private QuizResultQueryService quizResultQueryService;

    @BeforeEach
    void setUp() {
        quizResultRepository = mock(QuizResultRepository.class);
        quizPlaySessionRepository = mock(QuizPlaySessionRepository.class);
        quizSessionRepository = mock(QuizSessionRepository.class);
        subjectRepository = mock(SubjectRepository.class);
        userRepository = mock(UserRepository.class);
        questionRepository = mock(QuestionRepository.class);
        questionOptionRepository = mock(QuestionOptionRepository.class);
        questionAnswerRepository = mock(QuestionAnswerRepository.class);
        quizPlayAnswerRepository = mock(QuizPlayAnswerRepository.class);
        quizResultQueryService = new QuizResultQueryService(
                quizResultRepository,
                quizPlaySessionRepository,
                quizSessionRepository,
                subjectRepository,
                userRepository,
                questionRepository,
                questionOptionRepository,
                questionAnswerRepository,
                quizPlayAnswerRepository,
                new QuizResultResponseAssembler()
        );
    }

    @Test
    void getQuizResult_returns_saved_result_detail() {
        Long userId = 1L;
        User user = user(userId, 20, 400, 3);
        Subject subject = subject(10L, "subject-public-id", userId, "데이터베이스");
        QuizSession quizSession = quizSession(500L, "quiz-session-public-id", userId, subject.getId());
        QuizPlaySession playSession = playSession(700L, "client-session-id", quizSession.getId(), userId, subject.getId());
        Question question = question(900L, "question-public-id", quizSession.getId(), userId, subject.getId(), 1);
        QuestionOption correctOption = option(1000L, question.getId(), 1, "정답", true);
        QuestionOption wrongOption = option(1001L, question.getId(), 2, "오답", false);
        QuestionAnswer answer = answer(2000L, question.getId(), "1");
        QuizPlayAnswer playAnswer = playAnswer(playSession.getId(), question.getId(), userId, correctOption.getId(), true, false);
        QuizResult result = result(3000L, "result-public-id", playSession, quizSession, subject, 1, 1, 0, 0, 100);

        when(userRepository.findByIdAndDeletedAtIsNull(userId))
                .thenReturn(Optional.of(user));
        when(quizResultRepository.findByPublicIdAndUserId(result.getPublicId(), userId))
                .thenReturn(Optional.of(result));
        when(quizPlaySessionRepository.findByIdAndUserId(playSession.getId(), userId))
                .thenReturn(Optional.of(playSession));
        when(quizSessionRepository.findByIdAndUserIdAndDeletedAtIsNull(quizSession.getId(), userId))
                .thenReturn(Optional.of(quizSession));
        when(subjectRepository.findByIdAndUserIdAndDeletedAtIsNull(subject.getId(), userId))
                .thenReturn(Optional.of(subject));
        when(questionRepository.findAllByQuizSessionIdAndUserIdOrderByDisplayOrderAscIdAsc(quizSession.getId(), userId))
                .thenReturn(List.of(question));
        when(questionOptionRepository.findAllByQuestionIdInOrderByQuestionIdAscOptionNumberAsc(List.of(question.getId())))
                .thenReturn(List.of(correctOption, wrongOption));
        when(questionAnswerRepository.findAllByQuestionIdIn(List.of(question.getId())))
                .thenReturn(List.of(answer));
        when(quizPlayAnswerRepository.findAllByPlaySessionId(playSession.getId()))
                .thenReturn(List.of(playAnswer));

        QuizResultResponse response = quizResultQueryService.getQuizResult(userId, result.getPublicId());

        assertThat(response.getResultId()).isEqualTo(result.getPublicId());
        assertThat(response.getPlaySessionId()).isEqualTo(playSession.getClientSessionId());
        assertThat(response.getQuizSessionId()).isEqualTo(quizSession.getPublicId());
        assertThat(response.getSubjectName()).isEqualTo(subject.getName());
        assertThat(response.getCorrectCount()).isEqualTo(1);
        assertThat(response.getRewards().getDotoriEarned()).isEqualTo(1);
        assertThat(response.getRewards().getCurrentDotoriBalance()).isEqualTo(20);
        assertThat(response.getReviewItems()).hasSize(1);
        assertThat(response.getReviewItems().get(0).getQuestionId()).isEqualTo(question.getPublicId());
        assertThat(response.getReviewItems().get(0).getSelectedOptionId()).isEqualTo(String.valueOf(correctOption.getId()));
        assertThat(response.getReviewItems().get(0).getAnswerValue()).isEqualTo("1");
        assertThat(response.getReviewItems().get(0).getCorrectServer()).isTrue();
    }

    @Test
    void getQuizResult_throws_not_found_when_result_missing() {
        Long userId = 1L;
        User user = user(userId, 20, 400, 3);
        when(userRepository.findByIdAndDeletedAtIsNull(userId))
                .thenReturn(Optional.of(user));
        when(quizResultRepository.findByPublicIdAndUserId("missing-result-id", userId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> quizResultQueryService.getQuizResult(userId, "missing-result-id"))
                .isInstanceOf(CustomException.class)
                .extracting(exception -> ((CustomException) exception).getErrorCode())
                .isEqualTo(ErrorCode.QUIZ_RESULT_NOT_FOUND);
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

    private QuizSession quizSession(Long id, String publicId, Long userId, Long subjectId) {
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
                "completed",
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
        playSession.submit(430000);
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

    private <T> T newEntity(Class<T> type) {
        return org.springframework.beans.BeanUtils.instantiateClass(type);
    }
}
