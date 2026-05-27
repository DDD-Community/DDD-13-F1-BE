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

/**
 * 퀴즈 결과 엔티티
 */
@Entity
@Table(
        name = "quiz_results",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_quiz_results_public_id", columnNames = "public_id"),
                @UniqueConstraint(name = "uq_quiz_results_play_session_id", columnNames = "play_session_id")
        },
        indexes = {
                @Index(name = "idx_quiz_results_user_id_created_at", columnList = "user_id, created_at"),
                @Index(name = "idx_quiz_results_quiz_session_id", columnList = "quiz_session_id"),
                @Index(name = "idx_quiz_results_subject_id_created_at", columnList = "subject_id, created_at")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class QuizResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    Long id;

    @Column(name = "public_id", length = 36, nullable = false)
    String publicId;

    @Column(name = "play_session_id", nullable = false)
    Long playSessionId;

    @Column(name = "quiz_session_id", nullable = false)
    Long quizSessionId;

    @Column(name = "user_id", nullable = false)
    Long userId;

    @Column(name = "subject_id", nullable = false)
    Long subjectId;

    @Column(name = "total_count", nullable = false)
    Integer totalCount;

    @Column(name = "correct_count", nullable = false)
    Integer correctCount;

    @Column(name = "wrong_count", nullable = false)
    Integer wrongCount;

    @Column(name = "skip_count", nullable = false)
    Integer skipCount;

    @Column(name = "accuracy_pct", nullable = false)
    Integer accuracyPct;

    @Column(name = "elapsed_ms", nullable = false)
    Integer elapsedMs;

    @Column(name = "dotori_earned", nullable = false)
    Integer dotoriEarned;

    @Column(name = "xp_earned", nullable = false)
    Integer xpEarned;

    @Column(name = "is_leveled_up", nullable = false)
    Boolean leveledUp;

    @Column(name = "new_level")
    Integer newLevel;

    @Column(name = "is_score_matched", nullable = false)
    Boolean scoreMatched;

    @Column(name = "is_abuse_flagged", nullable = false)
    Boolean abuseFlagged;

    @CreatedDate
    @Column(name = "created_at", nullable = false)
    LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    LocalDateTime deletedAt;

    public static QuizResult create(
            String publicId,
            Long playSessionId,
            Long quizSessionId,
            Long userId,
            Long subjectId,
            Integer totalCount,
            Integer correctCount,
            Integer wrongCount,
            Integer skipCount,
            Integer accuracyPct,
            Integer elapsedMs,
            Integer dotoriEarned,
            Integer xpEarned,
            Boolean leveledUp,
            Integer newLevel,
            Boolean scoreMatched,
            Boolean abuseFlagged
    ) {
        QuizResult result = new QuizResult();
        result.publicId = publicId;
        result.playSessionId = playSessionId;
        result.quizSessionId = quizSessionId;
        result.userId = userId;
        result.subjectId = subjectId;
        result.totalCount = totalCount;
        result.correctCount = correctCount;
        result.wrongCount = wrongCount;
        result.skipCount = skipCount;
        result.accuracyPct = accuracyPct;
        result.elapsedMs = elapsedMs;
        result.dotoriEarned = dotoriEarned;
        result.xpEarned = xpEarned;
        result.leveledUp = leveledUp;
        result.newLevel = newLevel;
        result.scoreMatched = scoreMatched;
        result.abuseFlagged = abuseFlagged;
        result.deletedAt = null;
        return result;
    }
}
