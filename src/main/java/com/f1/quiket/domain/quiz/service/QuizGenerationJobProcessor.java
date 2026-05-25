package com.f1.quiket.domain.quiz.service;

import com.f1.quiket.domain.part.entity.Part;
import com.f1.quiket.domain.part.repository.PartRepository;
import com.f1.quiket.domain.quiz.dto.QuizAiGeneratedOption;
import com.f1.quiket.domain.quiz.dto.QuizAiGeneratedQuestion;
import com.f1.quiket.domain.quiz.dto.QuizAiGenerationPrompt;
import com.f1.quiket.domain.quiz.dto.QuizAiGenerationRequest;
import com.f1.quiket.domain.quiz.dto.QuizAiGenerationResponse;
import com.f1.quiket.domain.quiz.entity.Question;
import com.f1.quiket.domain.quiz.entity.QuestionAnswer;
import com.f1.quiket.domain.quiz.entity.QuestionOption;
import com.f1.quiket.domain.quiz.entity.QuizGenerationJob;
import com.f1.quiket.domain.quiz.entity.QuizSession;
import com.f1.quiket.domain.quiz.entity.QuizSessionScope;
import com.f1.quiket.domain.quiz.repository.QuestionAnswerRepository;
import com.f1.quiket.domain.quiz.repository.QuestionOptionRepository;
import com.f1.quiket.domain.quiz.repository.QuestionRepository;
import com.f1.quiket.domain.quiz.repository.QuizGenerationJobRepository;
import com.f1.quiket.domain.quiz.repository.QuizSessionRepository;
import com.f1.quiket.domain.quiz.repository.QuizSessionScopeRepository;
import com.f1.quiket.domain.subject.entity.Subject;
import com.f1.quiket.domain.subject.repository.SubjectRepository;
import com.f1.quiket.global.error.CustomException;
import com.f1.quiket.global.response.ErrorCode;
import com.f1.quiket.global.util.UuidV7Generator;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

@Service
@RequiredArgsConstructor
public class QuizGenerationJobProcessor {

    private static final String STATUS_IN_PROGRESS = "in_progress";
    private static final String FAIL_CODE_GENERATION_FAILED = "quiz_generation_failed";
    private static final String TIMEOUT_FAIL_REASON = "퀴즈 생성 작업이 제한 시간을 초과했습니다.";
    private static final int MAX_RETRY_COUNT = 3;
    private static final int FAIL_REASON_MAX_LENGTH = 500;

    private final QuizAiClient quizAiClient;
    private final QuizGenerationPromptBuilder promptBuilder;
    private final QuizAiResponseValidator responseValidator;
    private final QuizGenerationJobRepository quizGenerationJobRepository;
    private final QuizSessionRepository quizSessionRepository;
    private final QuizSessionScopeRepository quizSessionScopeRepository;
    private final SubjectRepository subjectRepository;
    private final PartRepository partRepository;
    private final QuestionRepository questionRepository;
    private final QuestionOptionRepository questionOptionRepository;
    private final QuestionAnswerRepository questionAnswerRepository;
    private final TransactionTemplate transactionTemplate;

    /**
     * 퀴즈 생성 작업 처리.
     *
     * <p>성공/실패 무관 항상 {@code true} 반환 — 호출 측({@code QuizGenerationWorker})이
     * 큐에서 메시지를 ack(삭제)하도록 함. 실패 케이스는 generation_job/quiz_session을
     * failed 상태로 마킹해 사용자 status 조회로 노출되며, 큐에는 남기지 않음 (자동 재시도 X).
     * 자동 재시도가 필요해지면 후속 이슈에서 {@code retryCount}/{@code is_retryable} 기반 재enqueue 도입.</p>
     */
    public boolean process(QuizGenerationQueueMessage message) {
        try {
            GenerationContext context = transactionTemplate.execute(status -> prepareContext(message));
            if (context == null) {
                return true;
            }

            QuizAiGenerationPrompt prompt = promptBuilder.build(context.request());
            QuizAiGenerationResponse response = quizAiClient.generate(prompt);
            responseValidator.validate(context.request(), response);

            transactionTemplate.executeWithoutResult(status -> completeGeneration(context, response));
            return true;
        } catch (Exception e) {
            transactionTemplate.executeWithoutResult(status -> failGeneration(message, e));
            return true;
        }
    }

    public int markTimedOutJobs() {
        Integer count = transactionTemplate.execute(status -> {
            List<QuizGenerationJob> jobs = quizGenerationJobRepository.findAllByStatusAndTimeoutAtBefore(
                    STATUS_IN_PROGRESS,
                    LocalDateTime.now()
            );
            jobs.forEach(this::markTimedOutJob);
            return jobs.size();
        });
        return count == null ? 0 : count;
    }

    private GenerationContext prepareContext(QuizGenerationQueueMessage message) {
        QuizGenerationJob job = quizGenerationJobRepository.findById(message.generationJobId())
                .orElse(null);
        if (job == null || job.isTerminalStatus()) {
            return null;
        }

        QuizSession quizSession = findQuizSession(message);
        Subject subject = subjectRepository
                .findByIdAndUserIdAndDeletedAtIsNull(quizSession.getSubjectId(), quizSession.getUserId())
                .orElseThrow(() -> new CustomException(ErrorCode.SUBJECT_NOT_FOUND));
        List<Part> parts = findScopeParts(quizSession);

        job.markInProgress();
        quizSession.markGenerationInProgress();

        QuizAiGenerationRequest request = new QuizAiGenerationRequest(
                subject,
                parts,
                quizSession.getQuizType(),
                quizSession.getChoiceCount(),
                quizSession.getQuestionCount(),
                quizSession.getPlayMode(),
                quizSession.getTimerEnabled(),
                quizSession.getTimerScope(),
                quizSession.getTimerSeconds(),
                quizSession.getDifficulty()
        );
        Map<String, Part> partsByPublicId = parts.stream()
                .collect(Collectors.toMap(Part::getPublicId, Function.identity()));
        return new GenerationContext(message.generationJobId(), quizSession.getId(), quizSession.getUserId(), request, partsByPublicId);
    }

    private QuizSession findQuizSession(QuizGenerationQueueMessage message) {
        return quizSessionRepository.findByIdAndUserIdAndDeletedAtIsNull(message.quizSessionId(), message.userId())
                .orElseThrow(() -> new CustomException(ErrorCode.QUIZ_SESSION_NOT_FOUND));
    }

    private List<Part> findScopeParts(QuizSession quizSession) {
        List<Long> partIds = quizSessionScopeRepository.findAllByQuizSessionId(quizSession.getId()).stream()
                .map(QuizSessionScope::getPartId)
                .toList();
        if (partIds.isEmpty()) {
            throw new CustomException(ErrorCode.QUIZ_SCOPE_INVALID);
        }

        List<Part> parts = partRepository.findAllByIdInAndUserIdAndDeletedAtIsNull(partIds, quizSession.getUserId());
        if (parts.size() != partIds.stream().distinct().count()) {
            throw new CustomException(ErrorCode.QUIZ_SCOPE_INVALID);
        }
        return parts;
    }

    private void completeGeneration(GenerationContext context, QuizAiGenerationResponse response) {
        QuizGenerationJob job = quizGenerationJobRepository.findById(context.generationJobId())
                .orElseThrow(() -> new CustomException(ErrorCode.QUIZ_SESSION_NOT_FOUND));
        if (job.isTerminalStatus()) {
            return;
        }

        QuizSession quizSession = quizSessionRepository.findByIdAndUserIdAndDeletedAtIsNull(
                context.quizSessionId(),
                context.userId()
        ).orElseThrow(() -> new CustomException(ErrorCode.QUIZ_SESSION_NOT_FOUND));

        List<QuizAiGeneratedQuestion> generatedQuestions = response.getQuestions();
        for (int i = 0; i < generatedQuestions.size(); i++) {
            saveQuestion(quizSession, context.partsByPublicId(), generatedQuestions.get(i), i + 1);
        }
        quizSession.markGenerationCompleted(generatedQuestions.size());
        job.markCompleted();
    }

    private void saveQuestion(
            QuizSession quizSession,
            Map<String, Part> partsByPublicId,
            QuizAiGeneratedQuestion generatedQuestion,
            int displayOrder
    ) {
        Part part = partsByPublicId.get(generatedQuestion.getPartId());
        if (part == null) {
            throw new CustomException(ErrorCode.QUIZ_SCOPE_INVALID);
        }

        Question savedQuestion = questionRepository.save(Question.create(
                UuidV7Generator.generate(),
                quizSession.getId(),
                quizSession.getUserId(),
                quizSession.getSubjectId(),
                part.getChapterId(),
                part.getId(),
                generatedQuestion.getQuestionType(),
                generatedQuestion.getDifficulty(),
                generatedQuestion.getBody(),
                generatedQuestion.getSummary(),
                generatedQuestion.getCorrectExplanation(),
                generatedQuestion.getIncorrectExplanation(),
                displayOrder
        ));
        saveOptions(savedQuestion, generatedQuestion);
        questionAnswerRepository.save(QuestionAnswer.create(savedQuestion.getId(), generatedQuestion.getAnswerValue()));
    }

    private void saveOptions(Question question, QuizAiGeneratedQuestion generatedQuestion) {
        List<QuizAiGeneratedOption> generatedOptions = generatedQuestion.getOptions();
        if (generatedOptions == null || generatedOptions.isEmpty()) {
            return;
        }

        List<QuestionOption> options = generatedOptions.stream()
                .map(option -> QuestionOption.create(
                        question.getId(),
                        option.getOptionNumber(),
                        option.getContent(),
                        String.valueOf(option.getOptionNumber()).equals(generatedQuestion.getAnswerValue())
                ))
                .toList();
        questionOptionRepository.saveAll(options);
    }

    private void failGeneration(QuizGenerationQueueMessage message, Exception e) {
        QuizGenerationJob job = quizGenerationJobRepository.findById(message.generationJobId())
                .orElse(null);
        if (job == null || job.isTerminalStatus()) {
            return;
        }

        job.increaseRetryCount();
        String failReason = truncateFailReason(resolveFailReason(e));
        job.markFailed(resolveFailCode(e), failReason, job.getRetryCount() < MAX_RETRY_COUNT);
        quizSessionRepository.findByIdAndUserIdAndDeletedAtIsNull(message.quizSessionId(), message.userId())
                .ifPresent(quizSession -> quizSession.markGenerationFailed(failReason));
    }

    private void markTimedOutJob(QuizGenerationJob job) {
        job.markTimeout(TIMEOUT_FAIL_REASON);
        quizSessionRepository.findByIdAndUserIdAndDeletedAtIsNull(job.getQuizSessionId(), job.getUserId())
                .ifPresent(quizSession -> quizSession.markGenerationFailed(TIMEOUT_FAIL_REASON));
    }

    private String resolveFailCode(Exception e) {
        if (e instanceof CustomException customException) {
            return customException.getErrorCode().getCode();
        }
        return FAIL_CODE_GENERATION_FAILED;
    }

    private String resolveFailReason(Exception e) {
        return e.getMessage() == null ? "퀴즈 생성 처리에 실패했습니다." : e.getMessage();
    }

    private String truncateFailReason(String failReason) {
        if (failReason.length() <= FAIL_REASON_MAX_LENGTH) {
            return failReason;
        }
        return failReason.substring(0, FAIL_REASON_MAX_LENGTH);
    }

    private record GenerationContext(
            Long generationJobId,
            Long quizSessionId,
            Long userId,
            QuizAiGenerationRequest request,
            Map<String, Part> partsByPublicId
    ) {
    }
}
