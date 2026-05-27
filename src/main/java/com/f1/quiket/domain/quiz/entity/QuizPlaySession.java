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
                @Index(name = "idx_quiz_play_sessions_parent_play_session_id", columnList = "parent_play_session_id"),
                @Index(name = "idx_quiz_play_sessions_subject_id_created_at", columnList = "subject_id, created_at")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class QuizPlaySession {

    private static final String PLAY_TYPE_FIRST = "first";
    private static final String PLAY_TYPE_RETRY_ALL = "retry_all";
    private static final String PLAY_TYPE_RETRY_WRONG = "retry_wrong";
    private static final String STATUS_IN_PROGRESS = "in_progress";
    private static final String STATUS_SUBMITTED = "submitted";

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

    @Column(name = "play_type", length = 20, nullable = false)
    String playType;

    @Column(name = "parent_play_session_id")
    Long parentPlaySessionId;

    @Column(name = "parent_quiz_session_id")
    Long parentQuizSessionId;

    @Column(name = "generation", nullable = false)
    Integer generation;

    @Column(name = "is_question_shuffled", nullable = false)
    Boolean questionShuffled;

    @Column(name = "is_option_shuffled", nullable = false)
    Boolean optionShuffled;

    @Column(name = "shuffle_seed", length = 100)
    String shuffleSeed;

    @Column(name = "status", length = 20, nullable = false)
    String status;

    @Column(name = "last_question_index", nullable = false)
    Integer lastQuestionIndex;

    @Column(name = "elapsed_ms", nullable = false)
    Integer elapsedMs;

    @Column(name = "submitted_at")
    LocalDateTime submittedAt;

    @CreatedDate
    @Column(name = "created_at", nullable = false)
    LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    LocalDateTime deletedAt;

    public static QuizPlaySession createFirst(
            String clientSessionId,
            Long quizSessionId,
            Long userId,
            Long subjectId,
            Boolean questionShuffled,
            Boolean optionShuffled,
            String shuffleSeed
    ) {
        QuizPlaySession playSession = createBase(
                clientSessionId,
                quizSessionId,
                userId,
                subjectId,
                PLAY_TYPE_FIRST,
                questionShuffled,
                optionShuffled,
                shuffleSeed
        );
        playSession.generation = 0;
        return playSession;
    }

    public static QuizPlaySession createRetryAll(
            String clientSessionId,
            Long quizSessionId,
            Long userId,
            Long subjectId,
            Boolean questionShuffled,
            Boolean optionShuffled,
            String shuffleSeed
    ) {
        QuizPlaySession playSession = createBase(
                clientSessionId,
                quizSessionId,
                userId,
                subjectId,
                PLAY_TYPE_RETRY_ALL,
                questionShuffled,
                optionShuffled,
                shuffleSeed
        );
        playSession.generation = 0;
        return playSession;
    }

    public static QuizPlaySession createRetryWrong(
            String clientSessionId,
            Long quizSessionId,
            Long userId,
            Long subjectId,
            Long parentPlaySessionId,
            Long parentQuizSessionId,
            Integer generation,
            Boolean questionShuffled,
            Boolean optionShuffled,
            String shuffleSeed
    ) {
        QuizPlaySession playSession = createBase(
                clientSessionId,
                quizSessionId,
                userId,
                subjectId,
                PLAY_TYPE_RETRY_WRONG,
                questionShuffled,
                optionShuffled,
                shuffleSeed
        );
        playSession.parentPlaySessionId = parentPlaySessionId;
        playSession.parentQuizSessionId = parentQuizSessionId;
        playSession.generation = generation;
        return playSession;
    }

    private static QuizPlaySession createBase(
            String clientSessionId,
            Long quizSessionId,
            Long userId,
            Long subjectId,
            String playType,
            Boolean questionShuffled,
            Boolean optionShuffled,
            String shuffleSeed
    ) {
        QuizPlaySession playSession = new QuizPlaySession();
        playSession.clientSessionId = clientSessionId;
        playSession.quizSessionId = quizSessionId;
        playSession.userId = userId;
        playSession.subjectId = subjectId;
        playSession.playType = playType;
        playSession.questionShuffled = Boolean.TRUE.equals(questionShuffled);
        playSession.optionShuffled = optionShuffled == null || Boolean.TRUE.equals(optionShuffled);
        playSession.shuffleSeed = shuffleSeed;
        playSession.status = STATUS_IN_PROGRESS;
        playSession.lastQuestionIndex = 0;
        playSession.elapsedMs = 0;
        return playSession;
    }

    public boolean isSameStartRequest(Long quizSessionId, Long userId, String playType) {
        return this.quizSessionId.equals(quizSessionId)
                && this.userId.equals(userId)
                && this.playType.equals(playType);
    }

    public boolean isSameRetryWrongRequest(Long parentPlaySessionId, Long parentQuizSessionId, Long userId) {
        return this.userId.equals(userId)
                && PLAY_TYPE_RETRY_WRONG.equals(this.playType)
                && this.parentPlaySessionId != null
                && this.parentPlaySessionId.equals(parentPlaySessionId)
                && this.parentQuizSessionId != null
                && this.parentQuizSessionId.equals(parentQuizSessionId);
    }

    public void submit(Integer elapsedMs) {
        this.status = STATUS_SUBMITTED;
        this.elapsedMs = elapsedMs;
        this.submittedAt = LocalDateTime.now();
    }
}
