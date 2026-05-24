package com.f1.quiket.domain.quiz.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

    @Column(name = "created_at", nullable = false)
    LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    LocalDateTime updatedAt;
}
