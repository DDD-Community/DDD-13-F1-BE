package com.f1.quiket.domain.quiz.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.f1.quiket.domain.part.entity.Part;
import com.f1.quiket.domain.quiz.dto.QuizAiGenerationPrompt;
import com.f1.quiket.domain.quiz.dto.QuizAiGenerationRequest;
import com.f1.quiket.domain.subject.entity.Subject;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class QuizGenerationPromptBuilderTest {

    private final QuizGenerationPromptBuilder promptBuilder = new QuizGenerationPromptBuilder();

    @Test
    void build_includes_subject_parts_and_quiz_options() {
        Subject subject = subject(10L, "데이터베이스", "exam");
        Part part = part(100L, "018f8c2e-aaaa-7b6a-b9f0-111111111111", 20L, "정규화", "정규화는 중복을 줄이는 과정입니다.");
        QuizAiGenerationRequest request = new QuizAiGenerationRequest(
                subject,
                List.of(part),
                "multiple_choice",
                4,
                3,
                "one_by_one",
                true,
                "per_question",
                60,
                "medium"
        );

        QuizAiGenerationPrompt prompt = promptBuilder.build(request);

        assertThat(prompt.systemMessage()).contains("JSON Schema");
        assertThat(prompt.userMessage())
                .contains("데이터베이스")
                .contains("exam")
                .contains("multiple_choice")
                .contains("문제 수: 3")
                .contains("객관식 보기 수: 4")
                .contains("per_question")
                .contains("018f8c2e-aaaa-7b6a-b9f0-111111111111")
                .contains("정규화는 중복을 줄이는 과정입니다.")
                .contains("questions 배열 길이는 요청 문제 수와 정확히 같아야 한다");
    }

    private Subject subject(Long id, String name, String purpose) {
        Subject subject = org.springframework.beans.BeanUtils.instantiateClass(Subject.class);
        ReflectionTestUtils.setField(subject, "id", id);
        ReflectionTestUtils.setField(subject, "name", name);
        ReflectionTestUtils.setField(subject, "purpose", purpose);
        return subject;
    }

    private Part part(Long id, String publicId, Long chapterId, String name, String content) {
        Part part = org.springframework.beans.BeanUtils.instantiateClass(Part.class);
        ReflectionTestUtils.setField(part, "id", id);
        ReflectionTestUtils.setField(part, "publicId", publicId);
        ReflectionTestUtils.setField(part, "chapterId", chapterId);
        ReflectionTestUtils.setField(part, "partNumber", 1);
        ReflectionTestUtils.setField(part, "name", name);
        ReflectionTestUtils.setField(part, "content", content);
        return part;
    }
}
