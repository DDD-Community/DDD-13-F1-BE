package com.f1.quiket.domain.quiz.entity;

import com.f1.quiket.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

/**
 * 퀴즈 세션 엔티티
 */
@Entity
@Table(
        name = "quiz_sessions",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_quiz_sessions_public_id", columnNames = "public_id")
        },
        indexes = {
                @Index(name = "idx_quiz_sessions_user_id_status", columnList = "user_id, status"),
                @Index(name = "idx_quiz_sessions_user_id_created_at", columnList = "user_id, created_at"),
                @Index(name = "idx_quiz_sessions_subject_id_created_at", columnList = "subject_id, created_at"),
                @Index(name = "idx_quiz_sessions_status_created_at", columnList = "status, created_at")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class QuizSession extends BaseEntity {

    @Column(name = "public_id", length = 36, nullable = false)
    String publicId;

    @Column(name = "user_id", nullable = false)
    Long userId;

    @Column(name = "subject_id", nullable = false)
    Long subjectId;

    @Column(name = "quiz_type", length = 20, nullable = false)
    String quizType;

    @Column(name = "choice_count")
    Integer choiceCount;

    @Column(name = "question_count", nullable = false)
    Integer questionCount;

    @Column(name = "play_mode", length = 20, nullable = false)
    String playMode;

    @Column(name = "timer_enabled", nullable = false)
    Boolean timerEnabled;

    @Column(name = "timer_scope", length = 20)
    String timerScope;

    @Column(name = "timer_seconds")
    Integer timerSeconds;

    @Column(name = "difficulty", length = 10, nullable = false)
    String difficulty;

    @Column(name = "status", length = 20, nullable = false)
    String status;

    @Column(name = "job_id", length = 100)
    String jobId;

    @Column(name = "fail_reason", length = 255)
    String failReason;

    @Column(name = "generated_count")
    Integer generatedCount;

    @Column(name = "completed_at")
    LocalDateTime completedAt;

    public static QuizSession create(
            String publicId,
            Long userId,
            Long subjectId,
            String quizType,
            Integer choiceCount,
            Integer questionCount,
            String playMode,
            Boolean timerEnabled,
            String timerScope,
            Integer timerSeconds,
            String difficulty,
            String status,
            String jobId
    ) {
        QuizSession quizSession = new QuizSession();
        quizSession.publicId = publicId;
        quizSession.userId = userId;
        quizSession.subjectId = subjectId;
        quizSession.quizType = quizType;
        quizSession.choiceCount = choiceCount;
        quizSession.questionCount = questionCount;
        quizSession.playMode = playMode;
        quizSession.timerEnabled = timerEnabled;
        quizSession.timerScope = timerScope;
        quizSession.timerSeconds = timerSeconds;
        quizSession.difficulty = difficulty;
        quizSession.status = status;
        quizSession.jobId = jobId;
        return quizSession;
    }
}
