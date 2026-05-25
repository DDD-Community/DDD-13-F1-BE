package com.f1.quiket.domain.quiz.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

/**
 * 퀴즈 문항 정답 엔티티
 */
@Entity
@Table(
        name = "question_answers",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_question_answers_question_id", columnNames = "question_id")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class QuestionAnswer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    Long id;

    @Column(name = "question_id", nullable = false)
    Long questionId;

    @Column(name = "answer_value", length = 10, nullable = false)
    String answerValue;

    public static QuestionAnswer create(Long questionId, String answerValue) {
        QuestionAnswer answer = new QuestionAnswer();
        answer.questionId = questionId;
        answer.answerValue = answerValue;
        return answer;
    }
}
