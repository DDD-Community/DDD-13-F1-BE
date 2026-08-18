package com.f1.quiket.domain.quiz.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.f1.quiket.domain.part.entity.Part;
import com.f1.quiket.domain.quiz.dto.QuizAiGenerationPrompt;
import com.f1.quiket.domain.quiz.dto.QuizAiGenerationRequest;
import com.f1.quiket.domain.subject.entity.Subject;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class QuizGenerationPromptBuilderTest {

    private final QuizGenerationPromptBuilder promptBuilder = new QuizGenerationPromptBuilder();

    @Test
    void build_includes_subject_parts_and_quiz_options() {
        Subject subject = subject(10L, "데이터베이스", "exam");
        Part part = part(100L, "018f8c2e-aaaa-7b6a-b9f0-111111111111", 20L, "정규화", "정규화는 중복을 줄이는 과정입니다.");
        QuizAiGenerationRequest request = request(subject, part, "medium");

        QuizAiGenerationPrompt prompt = promptBuilder.build(request);

        assertThat(prompt.systemMessage()).contains("JSON Schema");
        assertThat(prompt.userMessage())
                .contains("데이터베이스")
                .contains("exam")
                .contains("시험 유형: university")
                .contains("전공명: 통계학")
                .contains("multiple_choice")
                .contains("문제 수: 3")
                .contains("객관식 보기 수: 4")
                .contains("per_question")
                .contains("018f8c2e-aaaa-7b6a-b9f0-111111111111")
                .contains("정규화는 중복을 줄이는 과정입니다.")
                .contains("questions 배열 길이는 요청 문제 수와 정확히 같아야 한다")
                .contains("질문의 핵심 표현을 어미만 바꾸어 반복하지 않고")
                .contains("선택지끼리 의미가 중복되지 않으며")
                .contains("응답 전 모든 문항과 선택지가 위 품질 규칙을 충족하는지 자체 검수한다");
    }

    @Test
    void build_includes_difficulty_specific_guidelines() {
        Subject subject = subject(10L, "통계학", "exam");
        Part part = part(100L, "part-public-id", 20L, "추정", "통계적 추정은 표본으로 모집단을 판단합니다.");

        String easyPrompt = promptBuilder.build(request(subject, part, "easy")).userMessage();
        String mediumPrompt = promptBuilder.build(request(subject, part, "medium")).userMessage();
        String hardPrompt = promptBuilder.build(request(subject, part, "hard")).userMessage();

        assertThat(easyPrompt)
                .contains("표시 난이도: 쉬움")
                .contains("핵심 개념 80% / 기본 응용 20%")
                .contains("모든 문항의 difficulty 필드값: easy");
        assertThat(mediumPrompt)
                .contains("표시 난이도: 보통")
                .contains("핵심 개념 40% / 지엽 사실 40% / 기본 응용 20%")
                .contains("모든 문항의 difficulty 필드값: medium");
        assertThat(hardPrompt)
                .contains("표시 난이도: 어려움")
                .contains("지엽 사실 30% / 개념 응용 50% / 복합 사고 20%")
                .contains("실제 추론 단계와 적용 깊이를 높인다")
                .contains("모든 문항의 difficulty 필드값: hard");
    }

    private QuizAiGenerationRequest request(Subject subject, Part part, String difficulty) {
        return new QuizAiGenerationRequest(
                subject,
                Map.of("시험 유형", "university", "전공명", "통계학"),
                List.of(part),
                "multiple_choice",
                4,
                3,
                "one_by_one",
                true,
                "per_question",
                60,
                difficulty
        );
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
