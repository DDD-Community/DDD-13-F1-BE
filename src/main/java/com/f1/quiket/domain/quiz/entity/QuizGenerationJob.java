package com.f1.quiket.domain.quiz.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Table(
        name = "quiz_generation_jobs",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_quiz_generation_jobs_session_id", columnNames = "quiz_session_id")
        },
        indexes = {
                @Index(name = "idx_quiz_generation_jobs_user_id_status", columnList = "user_id, status"),
                @Index(name = "idx_quiz_generation_jobs_timeout_at", columnList = "timeout_at")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class QuizGenerationJob {

    private static final String STATUS_PENDING = "pending";
    private static final String STATUS_IN_PROGRESS = "in_progress";
    private static final String STATUS_COMPLETED = "completed";
    private static final String STATUS_FAILED = "failed";
    private static final String STATUS_TIMEOUT = "timeout";
    private static final int PROGRESS_PENDING = 0;
    private static final int PROGRESS_STARTED = 10;
    private static final int PROGRESS_DONE = 100;
    private static final long TIMEOUT_MINUTES = 10L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    Long id;

    @Column(name = "quiz_session_id", nullable = false)
    Long quizSessionId;

    @Column(name = "user_id", nullable = false)
    Long userId;

    @Column(name = "status", length = 20, nullable = false)
    String status = STATUS_PENDING;

    @Column(name = "progress_pct", nullable = false)
    Integer progressPct = PROGRESS_PENDING;

    @Column(name = "estimated_seconds")
    Integer estimatedSeconds;

    @Column(name = "estimated_finish_at")
    LocalDateTime estimatedFinishAt;

    @Column(name = "started_at")
    LocalDateTime startedAt;

    @Column(name = "completed_at")
    LocalDateTime completedAt;

    @Column(name = "timeout_at")
    LocalDateTime timeoutAt;

    @Column(name = "retry_count", nullable = false)
    Integer retryCount = 0;

    @Column(name = "is_retryable", nullable = false)
    boolean retryable = true;

    @Column(name = "fail_code", length = 50)
    String failCode;

    @Column(name = "fail_reason", length = 500)
    String failReason;

    @Column(name = "mq_message_id", length = 100)
    String mqMessageId;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    LocalDateTime updatedAt;

    public static QuizGenerationJob create(Long quizSessionId, Long userId, Integer estimatedSeconds) {
        QuizGenerationJob job = new QuizGenerationJob();
        job.quizSessionId = quizSessionId;
        job.userId = userId;
        job.estimatedSeconds = estimatedSeconds;
        return job;
    }

    public void assignMqMessageId(String mqMessageId) {
        this.mqMessageId = mqMessageId;
    }

    public void markInProgress() {
        LocalDateTime now = LocalDateTime.now();
        this.status = STATUS_IN_PROGRESS;
        this.progressPct = PROGRESS_STARTED;
        this.startedAt = now;
        this.timeoutAt = now.plusMinutes(TIMEOUT_MINUTES);
        if (this.estimatedSeconds != null) {
            this.estimatedFinishAt = now.plusSeconds(this.estimatedSeconds);
        }
    }

    public void markCompleted() {
        this.status = STATUS_COMPLETED;
        this.progressPct = PROGRESS_DONE;
        this.completedAt = LocalDateTime.now();
        this.failCode = null;
        this.failReason = null;
        this.retryable = false;
    }

    public void markFailed(String failCode, String failReason, boolean retryable) {
        this.status = STATUS_FAILED;
        this.progressPct = PROGRESS_DONE;
        this.completedAt = LocalDateTime.now();
        this.failCode = failCode;
        this.failReason = failReason;
        this.retryable = retryable;
    }

    public void markTimeout(String failReason) {
        this.status = STATUS_TIMEOUT;
        this.progressPct = PROGRESS_DONE;
        this.completedAt = LocalDateTime.now();
        this.failCode = "timeout";
        this.failReason = failReason;
        this.retryable = true;
    }

    public void increaseRetryCount() {
        this.retryCount++;
    }
}
