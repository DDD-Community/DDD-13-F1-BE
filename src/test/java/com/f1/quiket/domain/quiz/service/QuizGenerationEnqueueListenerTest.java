package com.f1.quiket.domain.quiz.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.f1.quiket.domain.quiz.entity.QuizGenerationJob;
import com.f1.quiket.domain.quiz.event.QuizGenerationEnqueueRequestedEvent;
import com.f1.quiket.domain.quiz.repository.QuizGenerationJobRepository;
import com.f1.quiket.global.error.CustomException;
import com.f1.quiket.global.response.ErrorCode;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class QuizGenerationEnqueueListenerTest {

    private QuizGenerationQueue quizGenerationQueue;
    private QuizGenerationJobRepository quizGenerationJobRepository;
    private QuizGenerationEnqueueListener quizGenerationEnqueueListener;

    @BeforeEach
    void setUp() {
        quizGenerationQueue = mock(QuizGenerationQueue.class);
        quizGenerationJobRepository = mock(QuizGenerationJobRepository.class);
        quizGenerationEnqueueListener = new QuizGenerationEnqueueListener(
                quizGenerationQueue,
                quizGenerationJobRepository
        );
    }

    @Test
    void handle_enqueues_and_assigns_message_id_on_success() {
        QuizGenerationJob generationJob = job(900L);
        QuizGenerationEnqueueRequestedEvent event = new QuizGenerationEnqueueRequestedEvent(message());

        when(quizGenerationQueue.enqueue(any(QuizGenerationQueueMessage.class)))
                .thenReturn("1748130000000-0");
        when(quizGenerationJobRepository.findById(900L)).thenReturn(Optional.of(generationJob));

        quizGenerationEnqueueListener.handle(event);

        assertThat(generationJob.getMqMessageId()).isEqualTo("1748130000000-0");
        assertThat(generationJob.getStatus()).isEqualTo("pending");
    }

    @Test
    void handle_marks_job_failed_when_enqueue_throws() {
        QuizGenerationJob generationJob = job(900L);
        QuizGenerationEnqueueRequestedEvent event = new QuizGenerationEnqueueRequestedEvent(message());

        when(quizGenerationQueue.enqueue(any(QuizGenerationQueueMessage.class)))
                .thenThrow(new CustomException(ErrorCode.SERVICE_UNAVAILABLE));
        when(quizGenerationJobRepository.findById(900L)).thenReturn(Optional.of(generationJob));

        quizGenerationEnqueueListener.handle(event);

        assertThat(generationJob.getStatus()).isEqualTo("failed");
        assertThat(generationJob.getFailCode()).isEqualTo("enqueue_failed");
        assertThat(generationJob.getFailReason()).isEqualTo("퀴즈 생성 큐 발행에 실패했습니다.");
        assertThat(generationJob.isRetryable()).isTrue();
        assertThat(generationJob.getMqMessageId()).isNull();
    }

    @Test
    void handle_skips_silently_when_job_missing_on_enqueue_success() {
        QuizGenerationEnqueueRequestedEvent event = new QuizGenerationEnqueueRequestedEvent(message());

        when(quizGenerationQueue.enqueue(any(QuizGenerationQueueMessage.class)))
                .thenReturn("1748130000000-0");
        when(quizGenerationJobRepository.findById(900L)).thenReturn(Optional.empty());

        // 매우 드문 케이스 — job이 사라진 상태로 listener 도달. 예외 던지지 않고 정상 종료
        quizGenerationEnqueueListener.handle(event);

        verify(quizGenerationJobRepository).findById(900L);
        verify(quizGenerationJobRepository, never()).save(any());
    }

    private QuizGenerationJob job(Long id) {
        QuizGenerationJob job = QuizGenerationJob.create(500L, 1L, null);
        ReflectionTestUtils.setField(job, "id", id);
        return job;
    }

    private QuizGenerationQueueMessage message() {
        return new QuizGenerationQueueMessage(
                900L,
                500L,
                "quiz-session-public-id",
                "quiz-job-public-id",
                1L
        );
    }
}
