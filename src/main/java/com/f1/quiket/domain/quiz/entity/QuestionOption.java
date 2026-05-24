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
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

/**
 * 퀴즈 문항 선택지 엔티티
 */
@Entity
@Table(
        name = "question_options",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_question_options_question_option", columnNames = {"question_id", "option_number"})
        },
        indexes = {
                @Index(name = "idx_question_options_question_id", columnList = "question_id")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class QuestionOption {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    Long id;

    @Column(name = "question_id", nullable = false)
    Long questionId;

    @Column(name = "option_number", nullable = false)
    Integer optionNumber;

    @Lob
    @Column(name = "content", nullable = false)
    String content;

    @Column(name = "is_correct", nullable = false)
    Boolean correct;
}
