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
                @Index(name = "idx_quiz_sessions_subject_id_created_at", columnList = "subject_id, created_at")
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

    @Column(name = "question_count", nullable = false)
    Integer questionCount;

    @Column(name = "status", length = 20, nullable = false)
    String status;

    @Column(name = "completed_at")
    LocalDateTime completedAt;
}
