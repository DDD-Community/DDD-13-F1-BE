package com.f1.quiket.domain.quiz.dto;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class QuizClientSessionIdValidationTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    void playStart_accepts_128_length_client_session_id() {
        QuizPlayStartRequest request = new QuizPlayStartRequest();
        ReflectionTestUtils.setField(request, "clientSessionId", repeated("a", 128));
        ReflectionTestUtils.setField(request, "playType", "first");

        Set<ConstraintViolation<QuizPlayStartRequest>> violations = validator.validate(request);

        assertThat(violations).isEmpty();
    }

    @Test
    void playStart_rejects_over_128_length_client_session_id() {
        QuizPlayStartRequest request = new QuizPlayStartRequest();
        ReflectionTestUtils.setField(request, "clientSessionId", repeated("a", 129));
        ReflectionTestUtils.setField(request, "playType", "first");

        Set<ConstraintViolation<QuizPlayStartRequest>> violations = validator.validate(request);

        assertThat(violations)
                .extracting(violation -> violation.getPropertyPath().toString())
                .contains("clientSessionId");
    }

    @Test
    void retry_accepts_128_length_client_session_id() {
        QuizRetryRequest request = new QuizRetryRequest();
        ReflectionTestUtils.setField(request, "clientSessionId", repeated("a", 128));

        Set<ConstraintViolation<QuizRetryRequest>> violations = validator.validate(request);

        assertThat(violations).isEmpty();
    }

    @Test
    void resultSubmit_accepts_128_length_client_session_id() {
        QuizResultSubmitRequest request = new QuizResultSubmitRequest();
        ReflectionTestUtils.setField(request, "clientSessionId", repeated("a", 128));
        ReflectionTestUtils.setField(request, "quizSessionId", "019e7210-e009-7439-9691-34407bb643b3");
        ReflectionTestUtils.setField(request, "playType", "first");
        ReflectionTestUtils.setField(request, "elapsedMs", 1000);
        ReflectionTestUtils.setField(request, "answers", List.of(answerItem()));

        Set<ConstraintViolation<QuizResultSubmitRequest>> violations = validator.validate(request);

        assertThat(violations).isEmpty();
    }

    @Test
    void resultSubmit_rejects_over_128_length_parent_play_session_id() {
        QuizResultSubmitRequest request = new QuizResultSubmitRequest();
        ReflectionTestUtils.setField(request, "clientSessionId", repeated("a", 128));
        ReflectionTestUtils.setField(request, "quizSessionId", "019e7210-e009-7439-9691-34407bb643b3");
        ReflectionTestUtils.setField(request, "playType", "retry_wrong");
        ReflectionTestUtils.setField(request, "parentPlaySessionId", repeated("b", 129));
        ReflectionTestUtils.setField(request, "elapsedMs", 1000);
        ReflectionTestUtils.setField(request, "answers", List.of(answerItem()));

        Set<ConstraintViolation<QuizResultSubmitRequest>> violations = validator.validate(request);

        assertThat(violations)
                .extracting(violation -> violation.getPropertyPath().toString())
                .contains("parentPlaySessionId");
    }

    private QuizAnswerSubmitItem answerItem() {
        QuizAnswerSubmitItem item = new QuizAnswerSubmitItem();
        ReflectionTestUtils.setField(item, "questionId", "019e7210-e009-7439-9691-34407bb643b4");
        ReflectionTestUtils.setField(item, "selectedValue", "O");
        ReflectionTestUtils.setField(item, "skipped", false);
        return item;
    }

    private String repeated(String value, int count) {
        return value.repeat(count);
    }
}
