package com.f1.quiket.domain.quiz.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.f1.quiket.domain.part.entity.Part;
import com.f1.quiket.domain.part.repository.PartRepository;
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
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.BeanUtils;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

@SuppressWarnings({"unchecked", "rawtypes"})
class QuizGenerationJobProcessorTest {

    private QuizAiClient quizAiClient;
    private QuizGenerationJobRepository quizGenerationJobRepository;
    private QuizSessionRepository quizSessionRepository;
    private QuizSessionScopeRepository quizSessionScopeRepository;
    private SubjectRepository subjectRepository;
    private PartRepository partRepository;
    private QuestionRepository questionRepository;
    private QuestionOptionRepository questionOptionRepository;
    private QuestionAnswerRepository questionAnswerRepository;
    private QuizGenerationJobProcessor processor;

    @BeforeEach
    void setUp() {
        quizAiClient = mock(QuizAiClient.class);
        quizGenerationJobRepository = mock(QuizGenerationJobRepository.class);
        quizSessionRepository = mock(QuizSessionRepository.class);
        quizSessionScopeRepository = mock(QuizSessionScopeRepository.class);
        subjectRepository = mock(SubjectRepository.class);
        partRepository = mock(PartRepository.class);
        questionRepository = mock(QuestionRepository.class);
        questionOptionRepository = mock(QuestionOptionRepository.class);
        questionAnswerRepository = mock(QuestionAnswerRepository.class);

        PlatformTransactionManager transactionManager = mock(PlatformTransactionManager.class);
        when(transactionManager.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
        doNothing().when(transactionManager).commit(any(TransactionStatus.class));
        doNothing().when(transactionManager).rollback(any(TransactionStatus.class));

        processor = new QuizGenerationJobProcessor(
                quizAiClient,
                new QuizGenerationPromptBuilder(),
                new QuizAiResponseValidator(),
                quizGenerationJobRepository,
                quizSessionRepository,
                quizSessionScopeRepository,
                subjectRepository,
                partRepository,
                questionRepository,
                questionOptionRepository,
                questionAnswerRepository,
                new TransactionTemplate(transactionManager)
        );
    }

    @Test
    void process_saves_generated_questions_and_marks_completed() {
        QuizGenerationJob job = job(900L, 500L, 1L);
        QuizSession quizSession = quizSession(500L, 1L, 10L, "pending");
        Subject subject = subject(10L, 1L);
        Part part = part(100L, "part-public-id", 200L, 10L, 1L);
        stubGenerationContext(job, quizSession, subject, part);
        when(quizAiClient.generate(any())).thenReturn(aiResponse());
        stubQuestionSave();

        boolean processed = processor.process(message());

        assertThat(processed).isTrue();
        assertThat(job.getStatus()).isEqualTo("completed");
        assertThat(job.getProgressPct()).isEqualTo(100);
        assertThat(job.isRetryable()).isFalse();
        assertThat(quizSession.getStatus()).isEqualTo("completed");
        assertThat(quizSession.getGeneratedCount()).isEqualTo(1);

        ArgumentCaptor<Question> questionCaptor = ArgumentCaptor.forClass(Question.class);
        verify(questionRepository).save(questionCaptor.capture());
        Question question = questionCaptor.getValue();
        assertThat(question.getQuizSessionId()).isEqualTo(500L);
        assertThat(question.getUserId()).isEqualTo(1L);
        assertThat(question.getSubjectId()).isEqualTo(10L);
        assertThat(question.getChapterId()).isEqualTo(200L);
        assertThat(question.getPartId()).isEqualTo(100L);
        assertThat(question.getQuestionType()).isEqualTo("multiple_choice");
        assertThat(question.getSummary()).isEqualTo("데이터모델링핵심");
        assertThat(question.getDisplayOrder()).isEqualTo(1);

        ArgumentCaptor<Iterable<QuestionOption>> optionsCaptor = ArgumentCaptor.forClass(Iterable.class);
        verify(questionOptionRepository).saveAll(optionsCaptor.capture());
        List<QuestionOption> options = toList(optionsCaptor.getValue());
        assertThat(options).hasSize(4);
        assertThat(options.get(0).getQuestionId()).isEqualTo(1000L);
        assertThat(options.get(0).getOptionNumber()).isEqualTo(1);
        assertThat(options.get(0).getCorrect()).isTrue();
        assertThat(options.get(1).getCorrect()).isFalse();

        ArgumentCaptor<QuestionAnswer> answerCaptor = ArgumentCaptor.forClass(QuestionAnswer.class);
        verify(questionAnswerRepository).save(answerCaptor.capture());
        assertThat(answerCaptor.getValue().getQuestionId()).isEqualTo(1000L);
        assertThat(answerCaptor.getValue().getAnswerValue()).isEqualTo("1");
    }

    @Test
    void process_marks_failed_when_ai_client_fails() {
        QuizGenerationJob job = job(900L, 500L, 1L);
        QuizSession quizSession = quizSession(500L, 1L, 10L, "pending");
        Subject subject = subject(10L, 1L);
        Part part = part(100L, "part-public-id", 200L, 10L, 1L);
        stubGenerationContext(job, quizSession, subject, part);
        when(quizAiClient.generate(any()))
                .thenThrow(new CustomException(ErrorCode.SERVICE_UNAVAILABLE, "AOAI 호출 실패"));

        boolean processed = processor.process(message());

        assertThat(processed).isTrue();
        assertThat(job.getStatus()).isEqualTo("failed");
        assertThat(job.getRetryCount()).isEqualTo(1);
        assertThat(job.getFailCode()).isEqualTo(ErrorCode.SERVICE_UNAVAILABLE.getCode());
        assertThat(job.getFailReason()).isEqualTo("AOAI 호출 실패");
        assertThat(job.isRetryable()).isTrue();
        assertThat(quizSession.getStatus()).isEqualTo("failed");
        assertThat(quizSession.getFailReason()).isEqualTo("AOAI 호출 실패");
        verifyNoInteractions(questionRepository, questionOptionRepository, questionAnswerRepository);
    }

    @Test
    void markTimedOutJobs_marks_job_timeout_and_session_failed() {
        QuizGenerationJob job = job(900L, 500L, 1L);
        job.markInProgress();
        ReflectionTestUtils.setField(job, "timeoutAt", LocalDateTime.now().minusSeconds(1));
        QuizSession quizSession = quizSession(500L, 1L, 10L, "in_progress");

        when(quizGenerationJobRepository.findAllByStatusAndTimeoutAtBefore(any(), any()))
                .thenReturn(List.of(job));
        when(quizSessionRepository.findByIdAndUserIdAndDeletedAtIsNull(500L, 1L))
                .thenReturn(Optional.of(quizSession));

        int count = processor.markTimedOutJobs();

        assertThat(count).isEqualTo(1);
        assertThat(job.getStatus()).isEqualTo("timeout");
        assertThat(job.getFailCode()).isEqualTo("timeout");
        assertThat(job.getFailReason()).isEqualTo("퀴즈 생성 작업이 제한 시간을 초과했습니다.");
        assertThat(quizSession.getStatus()).isEqualTo("failed");
        assertThat(quizSession.getFailReason()).isEqualTo("퀴즈 생성 작업이 제한 시간을 초과했습니다.");
    }

    private void stubGenerationContext(QuizGenerationJob job, QuizSession quizSession, Subject subject, Part part) {
        when(quizGenerationJobRepository.findById(900L)).thenReturn(Optional.of(job));
        when(quizSessionRepository.findByIdAndUserIdAndDeletedAtIsNull(500L, 1L)).thenReturn(Optional.of(quizSession));
        when(subjectRepository.findByIdAndUserIdAndDeletedAtIsNull(10L, 1L)).thenReturn(Optional.of(subject));
        when(quizSessionScopeRepository.findAllByQuizSessionId(500L))
                .thenReturn(List.of(QuizSessionScope.create(500L, 100L, 200L)));
        when(partRepository.findAllByIdInAndUserIdAndDeletedAtIsNull(List.of(100L), 1L))
                .thenReturn(List.of(part));
    }

    private void stubQuestionSave() {
        AtomicLong sequence = new AtomicLong(1000L);
        when(questionRepository.save(any(Question.class))).thenAnswer(invocation -> {
            Question question = invocation.getArgument(0);
            ReflectionTestUtils.setField(question, "id", sequence.getAndIncrement());
            return question;
        });
    }

    private QuizGenerationQueueMessage message() {
        return new QuizGenerationQueueMessage(900L, 500L, "quiz-session-public-id", "quiz-job-public-id", 1L);
    }

    private QuizGenerationJob job(Long id, Long quizSessionId, Long userId) {
        QuizGenerationJob job = QuizGenerationJob.create(quizSessionId, userId, null);
        ReflectionTestUtils.setField(job, "id", id);
        return job;
    }

    private QuizSession quizSession(Long id, Long userId, Long subjectId, String status) {
        QuizSession quizSession = BeanUtils.instantiateClass(QuizSession.class);
        ReflectionTestUtils.setField(quizSession, "id", id);
        ReflectionTestUtils.setField(quizSession, "publicId", "quiz-session-public-id");
        ReflectionTestUtils.setField(quizSession, "userId", userId);
        ReflectionTestUtils.setField(quizSession, "subjectId", subjectId);
        ReflectionTestUtils.setField(quizSession, "quizType", "multiple_choice");
        ReflectionTestUtils.setField(quizSession, "choiceCount", 4);
        ReflectionTestUtils.setField(quizSession, "questionCount", 1);
        ReflectionTestUtils.setField(quizSession, "playMode", "one_by_one");
        ReflectionTestUtils.setField(quizSession, "timerEnabled", false);
        ReflectionTestUtils.setField(quizSession, "difficulty", "medium");
        ReflectionTestUtils.setField(quizSession, "status", status);
        ReflectionTestUtils.setField(quizSession, "jobId", "quiz-job-public-id");
        return quizSession;
    }

    private Subject subject(Long id, Long userId) {
        Subject subject = BeanUtils.instantiateClass(Subject.class);
        ReflectionTestUtils.setField(subject, "id", id);
        ReflectionTestUtils.setField(subject, "publicId", "subject-public-id");
        ReflectionTestUtils.setField(subject, "userId", userId);
        ReflectionTestUtils.setField(subject, "name", "데이터 모델링");
        ReflectionTestUtils.setField(subject, "purpose", "exam");
        return subject;
    }

    private Part part(Long id, String publicId, Long chapterId, Long subjectId, Long userId) {
        Part part = BeanUtils.instantiateClass(Part.class);
        ReflectionTestUtils.setField(part, "id", id);
        ReflectionTestUtils.setField(part, "publicId", publicId);
        ReflectionTestUtils.setField(part, "chapterId", chapterId);
        ReflectionTestUtils.setField(part, "subjectId", subjectId);
        ReflectionTestUtils.setField(part, "userId", userId);
        ReflectionTestUtils.setField(part, "name", "데이터 모델링 개요");
        ReflectionTestUtils.setField(part, "partNumber", 1);
        ReflectionTestUtils.setField(part, "content", "데이터 모델링은 현실 세계의 데이터를 추상화하고 구조화하는 과정입니다.");
        return part;
    }

    private QuizAiGenerationResponse aiResponse() {
        try {
            return new ObjectMapper().readValue("""
                    {
                      "questions": [
                        {
                          "partId": "part-public-id",
                          "questionType": "multiple_choice",
                          "difficulty": "medium",
                          "summary": "데이터모델링핵심",
                          "body": "다음 중 데이터 모델링의 설명으로 올바른 것은?",
                          "options": [
                            {"optionNumber": 1, "content": "현실 세계의 데이터를 추상화한다"},
                            {"optionNumber": 2, "content": "서버 배포만 자동화한다"},
                            {"optionNumber": 3, "content": "UI 색상만 정리한다"},
                            {"optionNumber": 4, "content": "로그 파일만 삭제한다"}
                          ],
                          "answerValue": "1",
                          "correctExplanation": "데이터 모델링은 현실 데이터를 추상화합니다.",
                          "incorrectExplanation": "다른 선택지는 데이터 모델링의 핵심과 맞지 않습니다."
                        }
                      ]
                    }
                    """, QuizAiGenerationResponse.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }

    private <T> List<T> toList(Iterable<T> iterable) {
        List<T> values = new ArrayList<>();
        iterable.forEach(values::add);
        return values;
    }
}
