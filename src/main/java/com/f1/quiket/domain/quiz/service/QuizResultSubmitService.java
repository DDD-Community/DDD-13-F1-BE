package com.f1.quiket.domain.quiz.service;

import com.f1.quiket.domain.gamification.service.GamificationRewardService;
import com.f1.quiket.domain.gamification.service.QuizRewardResult;
import com.f1.quiket.domain.quiz.dto.QuizAnswerSubmitItem;
import com.f1.quiket.domain.quiz.dto.QuizResultResponse;
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
import com.f1.quiket.global.util.UuidV7Generator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
@Transactional
public class QuizResultSubmitService {

    private static final String QUIZ_SESSION_STATUS_COMPLETED = "completed";
    private static final String PLAY_TYPE_FIRST = "first";
    private static final String PLAY_TYPE_RETRY_ALL = "retry_all";
    private static final String PLAY_TYPE_RETRY_WRONG = "retry_wrong";
    private static final String QUESTION_TYPE_MULTIPLE_CHOICE = "multiple_choice";
    private static final String QUESTION_TYPE_OX = "ox";
    private static final int ABUSE_MISMATCH_THRESHOLD_PCT = 30;

    private final QuizSessionRepository quizSessionRepository;
    private final SubjectRepository subjectRepository;
    private final QuestionRepository questionRepository;
    private final QuestionOptionRepository questionOptionRepository;
    private final QuestionAnswerRepository questionAnswerRepository;
    private final QuizPlaySessionRepository quizPlaySessionRepository;
    private final QuizPlayAnswerRepository quizPlayAnswerRepository;
    private final QuizResultRepository quizResultRepository;
    private final UserRepository userRepository;
    private final GamificationRewardService gamificationRewardService;
    private final QuizResultResponseAssembler quizResultResponseAssembler;

    public QuizResultSubmitOutcome submit(Long userId, QuizResultSubmitRequest request) {
        User user = findUser(userId);
        QuizPlaySession playSession = findPlaySession(userId, request.getClientSessionId());
        QuizSession quizSession = findQuizSession(userId, request.getQuizSessionId());
        Subject subject = findSubject(userId, quizSession.getSubjectId());

        validateSubmitTarget(playSession, quizSession, request);

        List<Question> questions = findQuestions(userId, quizSession.getId());
        List<Long> questionIds = questions.stream()
                .map(Question::getId)
                .toList();
        Map<Long, List<QuestionOption>> optionsByQuestionId = getOptionsByQuestionId(questionIds);
        Map<Long, QuestionAnswer> answersByQuestionId = getAnswersByQuestionId(questionIds);

        return quizResultRepository.findByPlaySessionId(playSession.getId())
                .map(existing -> responseFromExisting(
                        existing,
                        playSession,
                        quizSession,
                        subject,
                        user,
                        questions,
                        optionsByQuestionId,
                        answersByQuestionId
                ))
                .orElseGet(() -> createResult(
                        userId,
                        request,
                        playSession,
                        quizSession,
                        subject,
                        user,
                        questions,
                        optionsByQuestionId,
                        answersByQuestionId
                ));
    }

    private User findUser(Long userId) {
        return userRepository.findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.AUTH_USER_NOT_FOUND));
    }

    private QuizPlaySession findPlaySession(Long userId, String clientSessionId) {
        return quizPlaySessionRepository.findByClientSessionId(clientSessionId)
                .filter(playSession -> playSession.getUserId().equals(userId))
                .orElseThrow(() -> new CustomException(ErrorCode.QUIZ_PLAY_SESSION_NOT_FOUND));
    }

    private QuizSession findQuizSession(Long userId, String quizSessionPublicId) {
        return quizSessionRepository.findByPublicIdAndUserIdAndDeletedAtIsNull(quizSessionPublicId, userId)
                .orElseThrow(() -> new CustomException(ErrorCode.QUIZ_SESSION_NOT_FOUND));
    }

    private Subject findSubject(Long userId, Long subjectId) {
        return subjectRepository.findByIdAndUserIdAndDeletedAtIsNull(subjectId, userId)
                .orElseThrow(() -> new CustomException(ErrorCode.SUBJECT_NOT_FOUND));
    }

    private void validateSubmitTarget(
            QuizPlaySession playSession,
            QuizSession quizSession,
            QuizResultSubmitRequest request
    ) {
        if (!QUIZ_SESSION_STATUS_COMPLETED.equals(quizSession.getStatus())) {
            throw new CustomException(ErrorCode.QUIZ_SESSION_NOT_COMPLETED);
        }
        if (!playSession.getQuizSessionId().equals(quizSession.getId())) {
            throw new CustomException(ErrorCode.QUIZ_OPTION_INVALID);
        }
        if (!playSession.getPlayType().equals(request.getPlayType())
                || !isSupportedPlayType(playSession.getPlayType())
                || StringUtils.hasText(request.getParentPlaySessionId())) {
            throw new CustomException(ErrorCode.QUIZ_OPTION_INVALID);
        }
    }

    private boolean isSupportedPlayType(String playType) {
        return PLAY_TYPE_FIRST.equals(playType)
                || PLAY_TYPE_RETRY_ALL.equals(playType)
                || PLAY_TYPE_RETRY_WRONG.equals(playType);
    }

    private List<Question> findQuestions(Long userId, Long quizSessionId) {
        List<Question> questions = questionRepository.findAllByQuizSessionIdAndUserIdOrderByDisplayOrderAscIdAsc(
                quizSessionId,
                userId
        );
        if (questions.isEmpty()) {
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR, "완료된 퀴즈 세션에 문항이 존재하지 않습니다.");
        }
        return questions;
    }

    private Map<Long, List<QuestionOption>> getOptionsByQuestionId(List<Long> questionIds) {
        return questionOptionRepository.findAllByQuestionIdInOrderByQuestionIdAscOptionNumberAsc(questionIds)
                .stream()
                .collect(Collectors.groupingBy(QuestionOption::getQuestionId));
    }

    private Map<Long, QuestionAnswer> getAnswersByQuestionId(List<Long> questionIds) {
        Map<Long, QuestionAnswer> answersByQuestionId = questionAnswerRepository.findAllByQuestionIdIn(questionIds)
                .stream()
                .collect(Collectors.toMap(QuestionAnswer::getQuestionId, Function.identity()));
        if (answersByQuestionId.size() != questionIds.size()) {
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR, "퀴즈 문항 정답 정보가 올바르지 않습니다.");
        }
        return answersByQuestionId;
    }

    private QuizResultSubmitOutcome responseFromExisting(
            QuizResult result,
            QuizPlaySession playSession,
            QuizSession quizSession,
            Subject subject,
            User user,
            List<Question> questions,
            Map<Long, List<QuestionOption>> optionsByQuestionId,
            Map<Long, QuestionAnswer> answersByQuestionId
    ) {
        List<QuizPlayAnswer> playAnswers = quizPlayAnswerRepository.findAllByPlaySessionId(playSession.getId());
        QuizResultResponse response = quizResultResponseAssembler.build(
                result,
                playSession,
                quizSession,
                subject,
                user,
                questions,
                optionsByQuestionId,
                answersByQuestionId,
                playAnswers
        );
        return new QuizResultSubmitOutcome(response, false);
    }

    private QuizResultSubmitOutcome createResult(
            Long userId,
            QuizResultSubmitRequest request,
            QuizPlaySession playSession,
            QuizSession quizSession,
            Subject subject,
            User user,
            List<Question> questions,
            Map<Long, List<QuestionOption>> optionsByQuestionId,
            Map<Long, QuestionAnswer> answersByQuestionId
    ) {
        Map<Long, QuizPlayAnswer> playAnswersByQuestionId = gradeAnswers(
                userId,
                request,
                playSession,
                questions,
                optionsByQuestionId,
                answersByQuestionId
        );
        List<QuizPlayAnswer> playAnswers = questions.stream()
                .map(question -> playAnswersByQuestionId.get(question.getId()))
                .toList();

        int totalCount = questions.size();
        int correctCount = (int) playAnswers.stream()
                .filter(QuizPlayAnswer::getCorrectServer)
                .count();
        int skipCount = (int) playAnswers.stream()
                .filter(QuizPlayAnswer::getSkipped)
                .count();
        int wrongCount = totalCount - correctCount - skipCount;
        int accuracyPct = correctCount * 100 / totalCount;
        int mismatchCount = calculateMismatchCount(playAnswers);
        boolean scoreMatched = mismatchCount == 0;
        boolean abuseFlagged = mismatchCount * 100 >= totalCount * ABUSE_MISMATCH_THRESHOLD_PCT;

        quizPlayAnswerRepository.saveAll(playAnswers);
        playSession.submit(request.getElapsedMs());
        QuizRewardResult reward = gamificationRewardService.applyQuizReward(
                user,
                playSession,
                quizSession,
                correctCount
        );
        QuizResult savedResult = quizResultRepository.save(QuizResult.create(
                UuidV7Generator.generate(),
                playSession.getId(),
                quizSession.getId(),
                userId,
                quizSession.getSubjectId(),
                totalCount,
                correctCount,
                wrongCount,
                skipCount,
                accuracyPct,
                request.getElapsedMs(),
                reward.dotoriEarned(),
                reward.xpEarned(),
                reward.leveledUp(),
                reward.newLevel(),
                scoreMatched,
                abuseFlagged
        ));

        QuizResultResponse response = quizResultResponseAssembler.build(
                savedResult,
                playSession,
                quizSession,
                subject,
                user,
                questions,
                optionsByQuestionId,
                answersByQuestionId,
                playAnswers
        );
        return new QuizResultSubmitOutcome(response, true);
    }

    private Map<Long, QuizPlayAnswer> gradeAnswers(
            Long userId,
            QuizResultSubmitRequest request,
            QuizPlaySession playSession,
            List<Question> questions,
            Map<Long, List<QuestionOption>> optionsByQuestionId,
            Map<Long, QuestionAnswer> answersByQuestionId
    ) {
        Map<String, Question> questionsByPublicId = questions.stream()
                .collect(Collectors.toMap(Question::getPublicId, Function.identity(), (left, right) -> left, LinkedHashMap::new));
        validateSubmittedQuestions(request.getAnswers(), questionsByPublicId);

        Map<Long, QuestionOption> optionsById = optionsByQuestionId.values().stream()
                .flatMap(List::stream)
                .collect(Collectors.toMap(QuestionOption::getId, Function.identity()));

        Map<Long, QuizPlayAnswer> playAnswersByQuestionId = new LinkedHashMap<>();
        for (QuizAnswerSubmitItem item : request.getAnswers()) {
            Question question = questionsByPublicId.get(item.getQuestionId());
            QuestionAnswer answer = answersByQuestionId.get(question.getId());
            GradedAnswer gradedAnswer = gradeAnswer(item, question, answer, optionsById);
            QuizPlayAnswer playAnswer = QuizPlayAnswer.create(
                    playSession.getId(),
                    question.getId(),
                    userId,
                    gradedAnswer.selectedOptionId(),
                    gradedAnswer.selectedValue(),
                    item.getCorrectClient(),
                    gradedAnswer.correct(),
                    item.getSkipped(),
                    item.getAnswerElapsedMs(),
                    item.getMarked()
            );
            playAnswersByQuestionId.put(question.getId(), playAnswer);
        }
        return playAnswersByQuestionId;
    }

    private void validateSubmittedQuestions(List<QuizAnswerSubmitItem> items, Map<String, Question> questionsByPublicId) {
        if (items.size() != questionsByPublicId.size()) {
            throw new CustomException(ErrorCode.QUIZ_OPTION_INVALID);
        }

        Set<String> submittedQuestionIds = new HashSet<>();
        for (QuizAnswerSubmitItem item : items) {
            if (!questionsByPublicId.containsKey(item.getQuestionId()) || !submittedQuestionIds.add(item.getQuestionId())) {
                throw new CustomException(ErrorCode.QUIZ_OPTION_INVALID);
            }
        }
    }

    private GradedAnswer gradeAnswer(
            QuizAnswerSubmitItem item,
            Question question,
            QuestionAnswer answer,
            Map<Long, QuestionOption> optionsById
    ) {
        if (Boolean.TRUE.equals(item.getSkipped())) {
            return new GradedAnswer(null, null, false);
        }
        if (QUESTION_TYPE_MULTIPLE_CHOICE.equals(question.getQuestionType())) {
            Long selectedOptionId = parseSelectedOptionId(item.getSelectedOptionId());
            QuestionOption selectedOption = optionsById.get(selectedOptionId);
            if (selectedOption == null || !selectedOption.getQuestionId().equals(question.getId())) {
                throw new CustomException(ErrorCode.QUIZ_OPTION_INVALID);
            }
            return new GradedAnswer(selectedOptionId, null, Boolean.TRUE.equals(selectedOption.getCorrect()));
        }
        if (QUESTION_TYPE_OX.equals(question.getQuestionType())) {
            String selectedValue = normalizeSelectedValue(item.getSelectedValue());
            if (!"O".equals(selectedValue) && !"X".equals(selectedValue)) {
                throw new CustomException(ErrorCode.QUIZ_OPTION_INVALID);
            }
            return new GradedAnswer(null, selectedValue, selectedValue.equals(answer.getAnswerValue()));
        }
        throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR, "퀴즈 문항 유형이 올바르지 않습니다.");
    }

    private Long parseSelectedOptionId(String selectedOptionId) {
        if (!StringUtils.hasText(selectedOptionId)) {
            throw new CustomException(ErrorCode.QUIZ_OPTION_INVALID);
        }
        try {
            return Long.parseLong(selectedOptionId);
        } catch (NumberFormatException e) {
            throw new CustomException(ErrorCode.QUIZ_OPTION_INVALID);
        }
    }

    private String normalizeSelectedValue(String selectedValue) {
        if (!StringUtils.hasText(selectedValue)) {
            throw new CustomException(ErrorCode.QUIZ_OPTION_INVALID);
        }
        return selectedValue.trim().toUpperCase();
    }

    private int calculateMismatchCount(List<QuizPlayAnswer> playAnswers) {
        return (int) playAnswers.stream()
                .filter(answer -> answer.getCorrectClient() != null)
                .filter(answer -> !answer.getCorrectClient().equals(answer.getCorrectServer()))
                .count();
    }

    private record GradedAnswer(Long selectedOptionId, String selectedValue, Boolean correct) {
    }
}
