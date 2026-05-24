package com.f1.quiket.domain.quiz.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

/**
 * 퀴즈 세션 출제 범위 엔티티
 */
@Entity
@Table(
        name = "quiz_session_scopes",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_quiz_session_scopes_session_part",
                        columnNames = {"quiz_session_id", "part_id"}
                )
        },
        indexes = {
                @Index(name = "idx_quiz_session_scopes_quiz_session_id", columnList = "quiz_session_id"),
                @Index(name = "idx_quiz_session_scopes_part_id", columnList = "part_id")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class QuizSessionScope {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    Long id;

    @Column(name = "quiz_session_id", nullable = false)
    Long quizSessionId;

    @Column(name = "part_id", nullable = false)
    Long partId;

    @Column(name = "chapter_id", nullable = false)
    Long chapterId;

    public static QuizSessionScope create(Long quizSessionId, Long partId, Long chapterId) {
        QuizSessionScope scope = new QuizSessionScope();
        scope.quizSessionId = quizSessionId;
        scope.partId = partId;
        scope.chapterId = chapterId;
        return scope;
    }
}
