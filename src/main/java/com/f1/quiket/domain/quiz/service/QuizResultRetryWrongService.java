package com.f1.quiket.domain.quiz.service;

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
import com.f1.quiket.domain.subject.repository.SubjectRepository;
import com.f1.quiket.global.error.CustomException;
import com.f1.quiket.global.response.ErrorCode;
import com.f1.quiket.global.util.UuidV7Generator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class QuizResultRetryWrongService {

    private static final String QUIZ_SESSION_STATUS_COMPLETED = "completed";

    private final QuizResultRepository quizResultRepository;
    private final QuizPlaySessionRepository quizPlaySessionRepository;
    private final QuizSessionRepository quizSessionRepository;
    private final SubjectRepository subjectRepository;
    private final QuestionRepository questionRepository;
    private final QuestionOptionRepository questionOptionRepository;
    private final QuestionAnswerRepository questionAnswerRepository;
    private final QuizPlayAnswerRepository quizPlayAnswerRepository;
    private final QuizSessionScopeRepository quizSessionScopeRepository;

    public QuizPlaySessionResponse retryWrong(Long userId, String resultPublicId, QuizRetryRequest request) {
        QuizResult result = quizResultRepository.findByPublicIdAndUserId(resultPublicId, userId)
                .orElseThrow(() -> new CustomException(ErrorCode.QUIZ_RESULT_NOT_FOUND));
        QuizPlaySession parentPlaySession = quizPlaySessionRepository.findByIdAndUserId(result.getPlaySessionId(), userId)
                .orElseThrow(() -> new CustomException(ErrorCode.QUIZ_PLAY_SESSION_NOT_FOUND));
        QuizSession parentQuizSession = quizSessionRepository.findByIdAndUserIdAndDeletedAtIsNull(result.getQuizSessionId(), userId)
                .orElseThrow(() -> new CustomException(ErrorCode.QUIZ_SESSION_NOT_FOUND));
        validateSubjectAlive(userId, parentQuizSession.getSubjectId());
        validateRetryable(parentQuizSession);

        return quizPlaySessionRepository.findByClientSessionId(request.getClientSessionId())
                .map(existing -> responseFromExisting(existing, parentPlaySession, parentQuizSession, userId))
                .orElseGet(() -> createRetryWrongPlaySession(userId, parentPlaySession, parentQuizSession, request));
    }

    private void validateSubjectAlive(Long userId, Long subjectId) {
        subjectRepository.findByIdAndUserIdAndDeletedAtIsNull(subjectId, userId)
                .orElseThrow(() -> new CustomException(ErrorCode.SUBJECT_NOT_FOUND));
    }

    private void validateRetryable(QuizSession quizSession) {
        if (!QUIZ_SESSION_STATUS_COMPLETED.equals(quizSession.getStatus())) {
            throw new CustomException(ErrorCode.QUIZ_SESSION_NOT_COMPLETED);
        }
    }

    private QuizPlaySessionResponse responseFromExisting(
            QuizPlaySession existing,
            QuizPlaySession parentPlaySession,
            QuizSession parentQuizSession,
            Long userId
    ) {
        if (!existing.isSameRetryWrongRequest(parentPlaySession.getId(), parentQuizSession.getId(), userId)) {
            throw new CustomException(ErrorCode.CONFLICT, "이미 다른 풀이 세션에서 사용 중인 clientSessionId입니다.");
        }
        QuizSession childQuizSession = quizSessionRepository
                .findByIdAndUserIdAndDeletedAtIsNull(existing.getQuizSessionId(), userId)
                .orElseThrow(() -> new CustomException(ErrorCode.QUIZ_SESSION_NOT_FOUND));
        return QuizPlaySessionResponse.of(existing, childQuizSession);
    }

    private QuizPlaySessionResponse createRetryWrongPlaySession(
            Long userId,
            QuizPlaySession parentPlaySession,
            QuizSession parentQuizSession,
            QuizRetryRequest request
    ) {
        List<Question> parentQuestions = findParentQuestions(userId, parentQuizSession.getId());
        List<QuizPlayAnswer> parentPlayAnswers = quizPlayAnswerRepository.findAllByPlaySessionId(parentPlaySession.getId());
        List<Question> wrongQuestions = findWrongQuestions(parentQuestions, parentPlayAnswers);
        Map<Long, List<QuestionOption>> optionsByQuestionId = getOptionsByQuestionId(parentQuestions);
        Map<Long, QuestionAnswer> answersByQuestionId = getAnswersByQuestionId(parentQuestions);

        try {
            QuizSession childQuizSession = createChildQuizSession(parentQuizSession, wrongQuestions);
            copyScopes(childQuizSession, wrongQuestions);
            copyQuestions(childQuizSession, wrongQuestions, optionsByQuestionId, answersByQuestionId);
            QuizPlaySession playSession = createPlaySession(userId, parentPlaySession, parentQuizSession, childQuizSession, request);
            QuizPlaySession savedPlaySession = quizPlaySessionRepository.save(playSession);
            return QuizPlaySessionResponse.of(savedPlaySession, childQuizSession);
        } catch (DataIntegrityViolationException e) {
            throw new CustomException(
                    ErrorCode.CONFLICT,
                    "이미 다른 풀이 세션에서 사용 중인 clientSessionId입니다.",
                    e
            );
        }
    }

    private List<Question> findParentQuestions(Long userId, Long quizSessionId) {
        List<Question> questions = questionRepository.findAllByQuizSessionIdAndUserIdOrderByDisplayOrderAscIdAsc(
                quizSessionId,
                userId
        );
        if (questions.isEmpty()) {
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR, "완료된 퀴즈 세션에 문항이 존재하지 않습니다.");
        }
        return questions;
    }

    private List<Question> findWrongQuestions(List<Question> parentQuestions, List<QuizPlayAnswer> parentPlayAnswers) {
        Set<Long> wrongQuestionIds = parentPlayAnswers.stream()
                .filter(answer -> Boolean.FALSE.equals(answer.getCorrectServer()))
                .map(QuizPlayAnswer::getQuestionId)
                .collect(Collectors.toSet());
        List<Question> wrongQuestions = parentQuestions.stream()
                .filter(question -> wrongQuestionIds.contains(question.getId()))
                .toList();
        if (wrongQuestions.isEmpty()) {
            throw new CustomException(ErrorCode.CONFLICT, "오답 문항이 없어 다시 풀 수 없습니다.");
        }
        if (wrongQuestions.size() != wrongQuestionIds.size()) {
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR, "풀이 답안의 문항 정보가 올바르지 않습니다.");
        }
        return wrongQuestions;
    }

    private Map<Long, List<QuestionOption>> getOptionsByQuestionId(List<Question> questions) {
        return questionOptionRepository.findAllByQuestionIdInOrderByQuestionIdAscOptionNumberAsc(questionIds(questions))
                .stream()
                .collect(Collectors.groupingBy(QuestionOption::getQuestionId));
    }

    private Map<Long, QuestionAnswer> getAnswersByQuestionId(List<Question> questions) {
        Map<Long, QuestionAnswer> answersByQuestionId = questionAnswerRepository.findAllByQuestionIdIn(questionIds(questions))
                .stream()
                .collect(Collectors.toMap(QuestionAnswer::getQuestionId, Function.identity()));
        if (answersByQuestionId.size() != questions.size()) {
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR, "퀴즈 문항 정답 정보가 올바르지 않습니다.");
        }
        return answersByQuestionId;
    }

    private List<Long> questionIds(List<Question> questions) {
        return questions.stream()
                .map(Question::getId)
                .toList();
    }

    private QuizSession createChildQuizSession(QuizSession parentQuizSession, List<Question> wrongQuestions) {
        QuizSession childQuizSession = QuizSession.create(
                UuidV7Generator.generate(),
                parentQuizSession.getUserId(),
                parentQuizSession.getSubjectId(),
                parentQuizSession.getQuizType(),
                parentQuizSession.getChoiceCount(),
                wrongQuestions.size(),
                parentQuizSession.getPlayMode(),
                parentQuizSession.getTimerEnabled(),
                parentQuizSession.getTimerScope(),
                parentQuizSession.getTimerSeconds(),
                parentQuizSession.getDifficulty(),
                QUIZ_SESSION_STATUS_COMPLETED,
                null
        );
        childQuizSession.markGenerationCompleted(wrongQuestions.size());
        return quizSessionRepository.save(childQuizSession);
    }

    private void copyScopes(QuizSession childQuizSession, List<Question> wrongQuestions) {
        Map<Long, Question> questionsByPartId = wrongQuestions.stream()
                .collect(Collectors.toMap(
                        Question::getPartId,
                        Function.identity(),
                        (first, ignored) -> first,
                        LinkedHashMap::new
                ));
        List<QuizSessionScope> scopes = questionsByPartId.values().stream()
                .map(question -> QuizSessionScope.create(
                        childQuizSession.getId(),
                        question.getPartId(),
                        question.getChapterId()
                ))
                .toList();
        quizSessionScopeRepository.saveAll(scopes);
    }

    private void copyQuestions(
            QuizSession childQuizSession,
            List<Question> wrongQuestions,
            Map<Long, List<QuestionOption>> optionsByQuestionId,
            Map<Long, QuestionAnswer> answersByQuestionId
    ) {
        int displayOrder = 1;
        for (Question parentQuestion : wrongQuestions) {
            Question childQuestion = questionRepository.save(Question.create(
                    UuidV7Generator.generate(),
                    childQuizSession.getId(),
                    parentQuestion.getUserId(),
                    parentQuestion.getSubjectId(),
                    parentQuestion.getChapterId(),
                    parentQuestion.getPartId(),
                    parentQuestion.getQuestionType(),
                    parentQuestion.getDifficulty(),
                    parentQuestion.getBody(),
                    parentQuestion.getSummary(),
                    parentQuestion.getCorrectExplanation(),
                    parentQuestion.getIncorrectExplanation(),
                    displayOrder++
            ));
            copyOptions(parentQuestion, childQuestion, optionsByQuestionId);
            copyAnswer(parentQuestion, childQuestion, answersByQuestionId);
        }
    }

    private void copyOptions(
            Question parentQuestion,
            Question childQuestion,
            Map<Long, List<QuestionOption>> optionsByQuestionId
    ) {
        List<QuestionOption> childOptions = optionsByQuestionId.getOrDefault(parentQuestion.getId(), List.of())
                .stream()
                .map(option -> QuestionOption.create(
                        childQuestion.getId(),
                        option.getOptionNumber(),
                        option.getContent(),
                        option.getCorrect()
                ))
                .toList();
        if (!childOptions.isEmpty()) {
            questionOptionRepository.saveAll(childOptions);
        }
    }

    private void copyAnswer(
            Question parentQuestion,
            Question childQuestion,
            Map<Long, QuestionAnswer> answersByQuestionId
    ) {
        QuestionAnswer parentAnswer = answersByQuestionId.get(parentQuestion.getId());
        questionAnswerRepository.save(QuestionAnswer.create(childQuestion.getId(), parentAnswer.getAnswerValue()));
    }

    private QuizPlaySession createPlaySession(
            Long userId,
            QuizPlaySession parentPlaySession,
            QuizSession parentQuizSession,
            QuizSession childQuizSession,
            QuizRetryRequest request
    ) {
        return QuizPlaySession.createRetryWrong(
                request.getClientSessionId(),
                childQuizSession.getId(),
                userId,
                childQuizSession.getSubjectId(),
                parentPlaySession.getId(),
                parentQuizSession.getId(),
                parentPlaySession.getGeneration() + 1,
                request.getQuestionShuffled(),
                request.getOptionShuffled(),
                request.getShuffleSeed()
        );
    }
}
