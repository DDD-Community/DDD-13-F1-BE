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
 * 퀴즈 풀이 세션 엔티티
 */
@Entity
@Table(
        name = "quiz_play_sessions",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_quiz_play_sessions_client_session_id", columnNames = "client_session_id")
        },
        indexes = {
                @Index(name = "idx_quiz_play_sessions_quiz_session_id", columnList = "quiz_session_id"),
                @Index(name = "idx_quiz_play_sessions_user_id_status", columnList = "user_id, status"),
                @Index(name = "idx_quiz_play_sessions_user_id_created_at", columnList = "user_id, created_at"),
                @Index(name = "idx_quiz_play_sessions_subject_id_created_at", columnList = "subject_id, created_at")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class QuizPlaySession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    Long id;

    @Column(name = "client_session_id", length = 36, nullable = false)
    String clientSessionId;

    @Column(name = "quiz_session_id", nullable = false)
    Long quizSessionId;

    @Column(name = "user_id", nullable = false)
    Long userId;

    @Column(name = "subject_id", nullable = false)
    Long subjectId;

    @Column(name = "status", length = 20, nullable = false)
    String status;

    @Column(name = "last_question_index", nullable = false)
    Integer lastQuestionIndex;

    @Column(name = "created_at", nullable = false)
    LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    LocalDateTime updatedAt;
}
