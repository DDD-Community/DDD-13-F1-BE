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
 * 퀴즈 풀이 답안 엔티티
 */
@Entity
@Table(
        name = "quiz_play_answers",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_quiz_play_answers_session_question", columnNames = {"play_session_id", "question_id"})
        },
        indexes = {
                @Index(name = "idx_quiz_play_answers_question_id", columnList = "question_id"),
                @Index(name = "idx_quiz_play_answers_user_id", columnList = "user_id")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class QuizPlayAnswer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    Long id;

    @Column(name = "play_session_id", nullable = false)
    Long playSessionId;

    @Column(name = "question_id", nullable = false)
    Long questionId;

    @Column(name = "user_id", nullable = false)
    Long userId;

    @Column(name = "selected_option_id")
    Long selectedOptionId;

    @Column(name = "selected_value", length = 10)
    String selectedValue;

    @Column(name = "is_correct_client")
    Boolean correctClient;

    @Column(name = "is_correct_server")
    Boolean correctServer;

    @Column(name = "is_skipped", nullable = false)
    Boolean skipped;

    @Column(name = "answer_elapsed_ms")
    Integer answerElapsedMs;

    @Column(name = "is_marked", nullable = false)
    Boolean marked;

    @CreatedDate
    @Column(name = "created_at", nullable = false)
    LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    LocalDateTime updatedAt;

    public static QuizPlayAnswer create(
            Long playSessionId,
            Long questionId,
            Long userId,
            Long selectedOptionId,
            String selectedValue,
            Boolean correctClient,
            Boolean correctServer,
            Boolean skipped,
            Integer answerElapsedMs,
            Boolean marked
    ) {
        QuizPlayAnswer answer = new QuizPlayAnswer();
        answer.playSessionId = playSessionId;
        answer.questionId = questionId;
        answer.userId = userId;
        answer.selectedOptionId = selectedOptionId;
        answer.selectedValue = selectedValue;
        answer.correctClient = correctClient;
        answer.correctServer = correctServer;
        answer.skipped = skipped;
        answer.answerElapsedMs = answerElapsedMs;
        answer.marked = Boolean.TRUE.equals(marked);
        return answer;
    }
}
