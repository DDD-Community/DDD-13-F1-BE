package com.f1.quiket.domain.quiz.service;

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
import com.f1.quiket.global.util.UuidV7Generator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * 퀴즈 세션 생성 비즈니스 로직
 */
@Service
@RequiredArgsConstructor
@Transactional
public class QuizSessionCreateService {

    private static final String QUIZ_TYPE_MULTIPLE_CHOICE = "multiple_choice";
    private static final String QUIZ_TYPE_OX = "ox";
    private static final String PLAY_MODE_ONE_BY_ONE = "one_by_one";
    private static final String PLAY_MODE_ALL_AT_ONCE = "all_at_once";
    private static final String TIMER_SCOPE_PER_QUESTION = "per_question";
    private static final String TIMER_SCOPE_TOTAL = "total";
    private static final String DIFFICULTY_MEDIUM = "medium";
    private static final String STATUS_PENDING = "pending";
    private static final List<String> ACTIVE_GENERATION_STATUSES = List.of("pending", "in_progress");
    private static final int CHARS_PER_QUESTION = 50;

    private final SubjectRepository subjectRepository;
    private final PartRepository partRepository;
    private final QuizSessionRepository quizSessionRepository;
    private final QuizSessionScopeRepository quizSessionScopeRepository;
    private final QuizGenerationLockStore quizGenerationLockStore;

    /**
     * 퀴즈 세션 생성
     */
    public QuizGenerationAcceptedResponse createQuizSession(Long userId, QuizCreateRequest request) {
        // 동시 요청(연타·재시도) 차단용 사용자 단위 분산락
        quizGenerationLockStore.acquire(userId);
        try {
            return createQuizSessionInternal(userId, request);
        } finally {
            quizGenerationLockStore.release(userId);
        }
    }

    private QuizGenerationAcceptedResponse createQuizSessionInternal(Long userId, QuizCreateRequest request) {
        validateActiveGeneration(userId);

        Subject subject = findSubject(userId, request.getSubjectId());
        QuizOptionValues optionValues = resolveOptions(request);
        List<Part> parts = findScopeParts(userId, subject.getId(), request.getPartIds());
        validateQuestionCountAgainstTextLength(userId, subject.getId(), request);

        QuizSession quizSession = QuizSession.create(
                UuidV7Generator.generate(),
                userId,
                subject.getId(),
                optionValues.quizType(),
                optionValues.choiceCount(),
                request.getQuestionCount(),
                optionValues.playMode(),
                optionValues.timerEnabled(),
                optionValues.timerScope(),
                optionValues.timerSeconds(),
                optionValues.difficulty(),
                STATUS_PENDING,
                createJobId()
        );

        QuizSession savedQuizSession = quizSessionRepository.save(quizSession);
        quizSessionScopeRepository.saveAll(createScopes(savedQuizSession.getId(), parts));

        return QuizGenerationAcceptedResponse.from(savedQuizSession);
    }

    /**
     * 50자당 1문제 정책 검증 (기능명세 QUIZ-GEN-001)
     */
    private void validateQuestionCountAgainstTextLength(Long userId, Long subjectId, QuizCreateRequest request) {
        long totalLength = partRepository.sumContentLengthByPublicIds(
                request.getPartIds(),
                subjectId,
                userId
        );
        long maxQuestionCount = totalLength / CHARS_PER_QUESTION;
        if (request.getQuestionCount() > maxQuestionCount) {
            throw new CustomException(ErrorCode.QUIZ_SCOPE_TEXT_INSUFFICIENT);
        }
    }

    private void validateActiveGeneration(Long userId) {
        if (quizSessionRepository.existsByUserIdAndStatusInAndDeletedAtIsNull(userId, ACTIVE_GENERATION_STATUSES)) {
            throw new CustomException(ErrorCode.QUIZ_GENERATION_IN_PROGRESS);
        }
    }

    private Subject findSubject(Long userId, String subjectPublicId) {
        return subjectRepository.findByPublicIdAndUserIdAndDeletedAtIsNull(subjectPublicId, userId)
                .orElseThrow(() -> new CustomException(ErrorCode.SUBJECT_NOT_FOUND));
    }

    private List<Part> findScopeParts(Long userId, Long subjectId, List<String> partPublicIds) {
        validateDuplicatePartIds(partPublicIds);

        List<Part> parts = partRepository.findAllByPublicIdInAndSubjectIdAndUserIdAndDeletedAtIsNull(
                partPublicIds,
                subjectId,
                userId
        );
        if (parts.size() != partPublicIds.size()) {
            throw new CustomException(ErrorCode.QUIZ_SCOPE_INVALID);
        }
        return parts;
    }

    private void validateDuplicatePartIds(List<String> partPublicIds) {
        Set<String> uniquePartIds = partPublicIds.stream().collect(Collectors.toSet());
        if (uniquePartIds.size() != partPublicIds.size()) {
            throw new CustomException(ErrorCode.QUIZ_SCOPE_INVALID);
        }
    }

    private QuizOptionValues resolveOptions(QuizCreateRequest request) {
        String quizType = request.getQuizType();
        validatePlayMode(request.getPlayMode());
        Integer choiceCount = resolveChoiceCount(quizType, request.getChoiceCount());
        Boolean timerEnabled = Boolean.TRUE.equals(request.getTimerEnabled());
        TimerValues timerValues = resolveTimer(request.getPlayMode(), timerEnabled, request.getTimerScope(), request.getTimerSeconds());
        String difficulty = StringUtils.hasText(request.getDifficulty()) ? request.getDifficulty() : DIFFICULTY_MEDIUM;
        validateDifficulty(difficulty);

        return new QuizOptionValues(
                quizType,
                choiceCount,
                request.getPlayMode(),
                timerEnabled,
                timerValues.timerScope(),
                timerValues.timerSeconds(),
                difficulty
        );
    }

    private void validatePlayMode(String playMode) {
        if (!PLAY_MODE_ONE_BY_ONE.equals(playMode) && !PLAY_MODE_ALL_AT_ONCE.equals(playMode)) {
            throw new CustomException(ErrorCode.QUIZ_OPTION_INVALID);
        }
    }

    private void validateDifficulty(String difficulty) {
        if (!Set.of("easy", "medium", "hard").contains(difficulty)) {
            throw new CustomException(ErrorCode.QUIZ_OPTION_INVALID);
        }
    }

    private Integer resolveChoiceCount(String quizType, Integer choiceCount) {
        if (QUIZ_TYPE_MULTIPLE_CHOICE.equals(quizType)) {
            Integer resolvedChoiceCount = choiceCount == null ? 4 : choiceCount;
            if (resolvedChoiceCount != 4 && resolvedChoiceCount != 5) {
                throw new CustomException(ErrorCode.QUIZ_OPTION_INVALID);
            }
            return resolvedChoiceCount;
        }

        if (QUIZ_TYPE_OX.equals(quizType)) {
            if (choiceCount != null) {
                throw new CustomException(ErrorCode.QUIZ_OPTION_INVALID);
            }
            return null;
        }

        throw new CustomException(ErrorCode.QUIZ_OPTION_INVALID);
    }

    private TimerValues resolveTimer(String playMode, Boolean timerEnabled, String timerScope, Integer timerSeconds) {
        if (!timerEnabled) {
            if (StringUtils.hasText(timerScope) || timerSeconds != null) {
                throw new CustomException(ErrorCode.QUIZ_OPTION_INVALID);
            }
            return new TimerValues(null, null);
        }

        if (!StringUtils.hasText(timerScope) || timerSeconds == null || timerSeconds < 1) {
            throw new CustomException(ErrorCode.QUIZ_OPTION_INVALID);
        }

        if (PLAY_MODE_ONE_BY_ONE.equals(playMode) && !TIMER_SCOPE_PER_QUESTION.equals(timerScope)) {
            throw new CustomException(ErrorCode.QUIZ_OPTION_INVALID);
        }

        if (PLAY_MODE_ALL_AT_ONCE.equals(playMode) && !TIMER_SCOPE_TOTAL.equals(timerScope)) {
            throw new CustomException(ErrorCode.QUIZ_OPTION_INVALID);
        }

        return new TimerValues(timerScope, timerSeconds);
    }

    private List<QuizSessionScope> createScopes(Long quizSessionId, List<Part> parts) {
        return parts.stream()
                .map(part -> QuizSessionScope.create(quizSessionId, part.getId(), part.getChapterId()))
                .toList();
    }

    // TODO: jobId — AI 생성 작업 도입 후 MQ message-id 등 실제 작업 식별자로 대체
    private String createJobId() {
        return "quiz-job-" + UuidV7Generator.generate();
    }

    private record TimerValues(String timerScope, Integer timerSeconds) {
    }

    private record QuizOptionValues(
            String quizType,
            Integer choiceCount,
            String playMode,
            Boolean timerEnabled,
            String timerScope,
            Integer timerSeconds,
            String difficulty
    ) {
    }
}
