package com.f1.quiket.domain.quiz.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

/**
 * 퀴즈 문항 엔티티
 */
@Entity
@Table(
        name = "questions",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_questions_public_id", columnNames = "public_id")
        },
        indexes = {
                @Index(name = "idx_questions_quiz_session_id_order", columnList = "quiz_session_id, display_order"),
                @Index(name = "idx_questions_part_id", columnList = "part_id")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Question {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    Long id;

    @Column(name = "public_id", length = 36, nullable = false)
    String publicId;

    @Column(name = "quiz_session_id", nullable = false)
    Long quizSessionId;

    @Column(name = "user_id", nullable = false)
    Long userId;

    @Column(name = "subject_id", nullable = false)
    Long subjectId;

    @Column(name = "chapter_id", nullable = false)
    Long chapterId;

    @Column(name = "part_id", nullable = false)
    Long partId;

    @Column(name = "question_type", length = 20, nullable = false)
    String questionType;

    @Column(name = "difficulty", length = 10, nullable = false)
    String difficulty;

    @Lob
    @Column(name = "body", nullable = false)
    String body;

    @Column(name = "summary", length = 20)
    String summary;

    @Lob
    @Column(name = "correct_explanation")
    String correctExplanation;

    @Lob
    @Column(name = "incorrect_explanation")
    String incorrectExplanation;

    @Column(name = "display_order", nullable = false)
    Integer displayOrder;

    @Column(name = "deleted_at")
    LocalDateTime deletedAt;

    public static Question create(
            String publicId,
            Long quizSessionId,
            Long userId,
            Long subjectId,
            Long chapterId,
            Long partId,
            String questionType,
            String difficulty,
            String body,
            String summary,
            String correctExplanation,
            String incorrectExplanation,
            Integer displayOrder
    ) {
        Question question = new Question();
        question.publicId = publicId;
        question.quizSessionId = quizSessionId;
        question.userId = userId;
        question.subjectId = subjectId;
        question.chapterId = chapterId;
        question.partId = partId;
        question.questionType = questionType;
        question.difficulty = difficulty;
        question.body = body;
        question.summary = summary;
        question.correctExplanation = correctExplanation;
        question.incorrectExplanation = incorrectExplanation;
        question.displayOrder = displayOrder;
        return question;
    }
}
