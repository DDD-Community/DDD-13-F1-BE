package com.f1.quiket.domain.quiz.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.f1.quiket.domain.part.entity.Part;
import com.f1.quiket.domain.quiz.dto.QuizAiGenerationRequest;
import com.f1.quiket.domain.quiz.dto.QuizAiGenerationResponse;
import com.f1.quiket.domain.quiz.exception.QuizAiResponseValidationException;
import com.f1.quiket.domain.subject.entity.Subject;
import com.f1.quiket.global.error.CustomException;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class QuizAiResponseValidatorTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final QuizAiResponseValidator validator = new QuizAiResponseValidator();

    @Test
    void validate_passes_multiple_choice_response() throws Exception {
        QuizAiGenerationRequest request = request("multiple_choice", 4, 1);
        QuizAiGenerationResponse response = response("""
                {
                  "questions": [
                    {
                      "partId": "part-public-id",
                      "questionType": "multiple_choice",
                      "difficulty": "medium",
                      "summary": "정규화 핵심 개념",
                      "body": "정규화의 목적은 무엇인가요?",
                      "options": [
                        {"optionNumber": 1, "content": "중복 최소화"},
                        {"optionNumber": 2, "content": "중복 최대화"},
                        {"optionNumber": 3, "content": "테이블 삭제"},
                        {"optionNumber": 4, "content": "인덱스 제거"}
                      ],
                      "answerValue": "1",
                      "correctExplanation": "정규화는 데이터 중복을 줄입니다.",
                      "incorrectExplanation": "다른 선택지는 정규화 목적과 다릅니다."
                    }
                  ]
                }
                """);

        validator.validate(request, response);
    }

    @Test
    void validate_passes_ox_response() throws Exception {
        QuizAiGenerationRequest request = request("ox", null, 1);
        QuizAiGenerationResponse response = response("""
                {
                  "questions": [
                    {
                      "partId": "part-public-id",
                      "questionType": "ox",
                      "difficulty": "medium",
                      "summary": "정규화 중복 감소",
                      "body": "정규화는 데이터 중복을 줄인다.",
                      "options": [],
                      "answerValue": "O",
                      "correctExplanation": "정규화는 중복 최소화를 돕습니다.",
                      "incorrectExplanation": "중복을 늘리는 과정이 아닙니다."
                    }
                  ]
                }
                """);

        validator.validate(request, response);
    }

    @Test
    void validate_throws_when_part_id_is_out_of_scope() throws Exception {
        QuizAiGenerationRequest request = request("multiple_choice", 4, 1);
        QuizAiGenerationResponse response = response("""
                {
                  "questions": [
                    {
                      "partId": "other-part-id",
                      "questionType": "multiple_choice",
                      "difficulty": "medium",
                      "summary": "정규화 핵심 개념",
                      "body": "정규화의 목적은 무엇인가요?",
                      "options": [
                        {"optionNumber": 1, "content": "중복 최소화"},
                        {"optionNumber": 2, "content": "중복 최대화"},
                        {"optionNumber": 3, "content": "테이블 삭제"},
                        {"optionNumber": 4, "content": "인덱스 제거"}
                      ],
                      "answerValue": "1",
                      "correctExplanation": "정규화는 데이터 중복을 줄입니다.",
                      "incorrectExplanation": "다른 선택지는 정규화 목적과 다릅니다."
                    }
                  ]
                }
                """);

        assertThatThrownBy(() -> validator.validate(request, response))
                .isInstanceOf(CustomException.class)
                .hasMessage("출제 범위 밖 partId가 포함되었습니다.");
    }

    @Test
    void validate_throws_when_question_count_differs() throws Exception {
        QuizAiGenerationRequest request = request("multiple_choice", 4, 2);
        QuizAiGenerationResponse response = response("""
                {
                  "questions": []
                }
                """);

        assertThatThrownBy(() -> validator.validate(request, response))
                .isInstanceOf(CustomException.class)
                .hasMessage("AI 퀴즈 문항 수가 요청과 다릅니다.");
    }

    @Test
    void validate_throws_when_question_difficulty_differs_from_request() throws Exception {
        QuizAiGenerationRequest request = request("multiple_choice", 4, 1, "easy");
        QuizAiGenerationResponse response = response("""
                {
                  "questions": [
                    {
                      "partId": "part-public-id",
                      "questionType": "multiple_choice",
                      "difficulty": "hard",
                      "summary": "정규화 핵심 개념",
                      "body": "정규화의 목적은 무엇인가요?",
                      "options": [
                        {"optionNumber": 1, "content": "중복 최소화"},
                        {"optionNumber": 2, "content": "중복 최대화"},
                        {"optionNumber": 3, "content": "테이블 삭제"},
                        {"optionNumber": 4, "content": "인덱스 제거"}
                      ],
                      "answerValue": "1",
                      "correctExplanation": "정규화는 데이터 중복을 줄입니다.",
                      "incorrectExplanation": "다른 선택지는 정규화 목적과 다릅니다."
                    }
                  ]
                }
                """);

        assertThatThrownBy(() -> validator.validate(request, response))
                .isInstanceOf(CustomException.class)
                .hasMessage("요청과 다른 난이도의 문항이 포함되었습니다.");
    }

    @Test
    void validate_throws_when_option_repeats_question_wording() throws Exception {
        QuizAiGenerationRequest request = request("multiple_choice", 4, 1);
        QuizAiGenerationResponse response = response("""
                {
                  "questions": [
                    {
                      "partId": "part-public-id",
                      "questionType": "multiple_choice",
                      "difficulty": "medium",
                      "summary": "통계학 불확실성 이해",
                      "body": "통계학에서 불확실성을 다루는 이유는 무엇인가?",
                      "options": [
                        {"optionNumber": 1, "content": "불확실성을 다루기 위해"},
                        {"optionNumber": 2, "content": "표본으로 모집단 특성을 추론하기 위해"},
                        {"optionNumber": 3, "content": "모든 관측값을 동일하게 만들기 위해"},
                        {"optionNumber": 4, "content": "자료 수집을 생략하기 위해"}
                      ],
                      "answerValue": "1",
                      "correctExplanation": "통계학은 불확실한 자료를 분석합니다.",
                      "incorrectExplanation": "다른 선택지는 통계적 분석 목적과 다릅니다."
                    }
                  ]
                }
                """);

        assertThatThrownBy(() -> validator.validate(request, response))
                .isInstanceOf(QuizAiResponseValidationException.class)
                .hasMessage("질문 표현을 반복하는 순환형 객관식 선택지가 포함되었습니다.");
    }

    @Test
    void validate_throws_when_normalized_options_are_duplicated() throws Exception {
        QuizAiGenerationRequest request = request("multiple_choice", 4, 1);
        QuizAiGenerationResponse response = response("""
                {
                  "questions": [
                    {
                      "partId": "part-public-id",
                      "questionType": "multiple_choice",
                      "difficulty": "medium",
                      "summary": "정규화 핵심 개념",
                      "body": "정규화의 주된 효과는 무엇인가요?",
                      "options": [
                        {"optionNumber": 1, "content": "데이터 중복 최소화"},
                        {"optionNumber": 2, "content": "데이터 중복 최소화!"},
                        {"optionNumber": 3, "content": "테이블 무조건 삭제"},
                        {"optionNumber": 4, "content": "인덱스 전체 제거"}
                      ],
                      "answerValue": "1",
                      "correctExplanation": "정규화는 데이터 중복을 줄입니다.",
                      "incorrectExplanation": "다른 선택지는 정규화 목적과 다릅니다."
                    }
                  ]
                }
                """);

        assertThatThrownBy(() -> validator.validate(request, response))
                .isInstanceOf(QuizAiResponseValidationException.class)
                .hasMessage("동일한 객관식 선택지가 중복되었습니다.");
    }

    @Test
    void validate_throws_when_generic_option_is_included() throws Exception {
        QuizAiGenerationRequest request = request("multiple_choice", 4, 1);
        QuizAiGenerationResponse response = response("""
                {
                  "questions": [
                    {
                      "partId": "part-public-id",
                      "questionType": "multiple_choice",
                      "difficulty": "medium",
                      "summary": "정규화 핵심 개념",
                      "body": "정규화의 주된 효과는 무엇인가요?",
                      "options": [
                        {"optionNumber": 1, "content": "데이터 중복 최소화"},
                        {"optionNumber": 2, "content": "위의 모든 것"},
                        {"optionNumber": 3, "content": "테이블 무조건 삭제"},
                        {"optionNumber": 4, "content": "인덱스 전체 제거"}
                      ],
                      "answerValue": "1",
                      "correctExplanation": "정규화는 데이터 중복을 줄입니다.",
                      "incorrectExplanation": "다른 선택지는 정규화 목적과 다릅니다."
                    }
                  ]
                }
                """);

        assertThatThrownBy(() -> validator.validate(request, response))
                .isInstanceOf(QuizAiResponseValidationException.class)
                .hasMessage("포괄적이거나 무의미한 객관식 선택지가 포함되었습니다.");
    }

    @Test
    void validate_throws_when_normalized_question_bodies_are_duplicated() throws Exception {
        QuizAiGenerationRequest request = request("ox", null, 2);
        QuizAiGenerationResponse response = response("""
                {
                  "questions": [
                    {
                      "partId": "part-public-id",
                      "questionType": "ox",
                      "difficulty": "medium",
                      "summary": "정규화 중복 감소",
                      "body": "정규화는 데이터 중복을 줄인다.",
                      "options": [],
                      "answerValue": "O",
                      "correctExplanation": "정규화는 중복 최소화를 돕습니다.",
                      "incorrectExplanation": "중복을 늘리는 과정이 아닙니다."
                    },
                    {
                      "partId": "part-public-id",
                      "questionType": "ox",
                      "difficulty": "medium",
                      "summary": "정규화 중복 제어",
                      "body": "정규화는 데이터 중복을 줄인다!",
                      "options": [],
                      "answerValue": "O",
                      "correctExplanation": "정규화는 중복 최소화를 돕습니다.",
                      "incorrectExplanation": "중복을 늘리는 과정이 아닙니다."
                    }
                  ]
                }
                """);

        assertThatThrownBy(() -> validator.validate(request, response))
                .isInstanceOf(QuizAiResponseValidationException.class)
                .hasMessage("동일한 문항 본문이 중복되었습니다.");
    }

    @Test
    void validate_throws_when_question_matches_historical_question() throws Exception {
        QuizAiGenerationRequest request = request(
                "multiple_choice",
                4,
                1,
                "medium",
                List.of("정규화의 주된 효과는 무엇인가요?")
        );
        QuizAiGenerationResponse response = response("""
                {
                  "questions": [
                    {
                      "partId": "part-public-id",
                      "questionType": "multiple_choice",
                      "difficulty": "medium",
                      "summary": "정규화 핵심 효과",
                      "body": "정규화의 주된 효과는 무엇인가요!",
                      "options": [
                        {"optionNumber": 1, "content": "데이터 중복 최소화"},
                        {"optionNumber": 2, "content": "테이블 전체 삭제"},
                        {"optionNumber": 3, "content": "인덱스 무조건 제거"},
                        {"optionNumber": 4, "content": "서버 배포 자동화"}
                      ],
                      "answerValue": "1",
                      "correctExplanation": "정규화는 데이터 중복을 줄입니다.",
                      "incorrectExplanation": "다른 선택지는 정규화 목적과 다릅니다."
                    }
                  ]
                }
                """);

        assertThatThrownBy(() -> validator.validate(request, response))
                .isInstanceOf(QuizAiResponseValidationException.class)
                .hasMessage("과거에 출제된 문항과 중복됩니다.");
    }

    @Test
    void validate_throws_when_question_is_highly_similar_to_historical_question() throws Exception {
        QuizAiGenerationRequest request = request(
                "multiple_choice",
                4,
                1,
                "medium",
                List.of("정규화가 데이터 중복을 줄이는 이유는 무엇인가?")
        );
        QuizAiGenerationResponse response = response("""
                {
                  "questions": [
                    {
                      "partId": "part-public-id",
                      "questionType": "multiple_choice",
                      "difficulty": "medium",
                      "summary": "정규화 중복 제어",
                      "body": "데이터 중복을 줄이기 위해 정규화를 수행하는 이유는 무엇인가?",
                      "options": [
                        {"optionNumber": 1, "content": "갱신 이상을 줄이고 일관성을 높이기 위해"},
                        {"optionNumber": 2, "content": "모든 테이블을 하나로 합치기 위해"},
                        {"optionNumber": 3, "content": "인덱스를 전부 제거하기 위해"},
                        {"optionNumber": 4, "content": "네트워크 지연을 없애기 위해"}
                      ],
                      "answerValue": "1",
                      "correctExplanation": "정규화는 중복과 갱신 이상을 줄입니다.",
                      "incorrectExplanation": "다른 선택지는 정규화 목적과 다릅니다."
                    }
                  ]
                }
                """);

        assertThatThrownBy(() -> validator.validate(request, response))
                .isInstanceOf(QuizAiResponseValidationException.class)
                .hasMessage("과거에 출제된 문항과 지나치게 유사합니다.");
    }

    @Test
    void validate_passes_when_historical_question_covers_different_detail() throws Exception {
        QuizAiGenerationRequest request = request(
                "multiple_choice",
                4,
                1,
                "medium",
                List.of("데이터베이스 정규화에서 제2정규형의 조건은 무엇인가?")
        );
        QuizAiGenerationResponse response = response("""
                {
                  "questions": [
                    {
                      "partId": "part-public-id",
                      "questionType": "multiple_choice",
                      "difficulty": "medium",
                      "summary": "제3정규형 조건 구분",
                      "body": "데이터베이스 정규화에서 제3정규형의 조건은 무엇인가?",
                      "options": [
                        {"optionNumber": 1, "content": "이행적 함수 종속 제거"},
                        {"optionNumber": 2, "content": "부분 함수 종속 유지"},
                        {"optionNumber": 3, "content": "후보키 전체 삭제"},
                        {"optionNumber": 4, "content": "모든 속성의 중복 허용"}
                      ],
                      "answerValue": "1",
                      "correctExplanation": "제3정규형은 이행적 함수 종속을 제거합니다.",
                      "incorrectExplanation": "다른 선택지는 제3정규형 조건과 다릅니다."
                    }
                  ]
                }
                """);

        validator.validate(request, response);
    }

    private QuizAiGenerationRequest request(String quizType, Integer choiceCount, Integer questionCount) {
        return request(quizType, choiceCount, questionCount, "medium");
    }

    private QuizAiGenerationRequest request(
            String quizType,
            Integer choiceCount,
            Integer questionCount,
            String difficulty
    ) {
        return request(quizType, choiceCount, questionCount, difficulty, List.of());
    }

    private QuizAiGenerationRequest request(
            String quizType,
            Integer choiceCount,
            Integer questionCount,
            String difficulty,
            List<String> excludedQuestionBodies
    ) {
        return new QuizAiGenerationRequest(
                subject(),
                Map.of(),
                List.of(part()),
                "quiz-session-public-id",
                excludedQuestionBodies,
                quizType,
                choiceCount,
                questionCount,
                "one_by_one",
                false,
                null,
                null,
                difficulty
        );
    }

    private QuizAiGenerationResponse response(String json) throws JsonProcessingException {
        return objectMapper.readValue(json, QuizAiGenerationResponse.class);
    }

    private Subject subject() {
        Subject subject = org.springframework.beans.BeanUtils.instantiateClass(Subject.class);
        ReflectionTestUtils.setField(subject, "name", "데이터베이스");
        ReflectionTestUtils.setField(subject, "purpose", "exam");
        return subject;
    }

    private Part part() {
        Part part = org.springframework.beans.BeanUtils.instantiateClass(Part.class);
        ReflectionTestUtils.setField(part, "publicId", "part-public-id");
        ReflectionTestUtils.setField(part, "chapterId", 10L);
        ReflectionTestUtils.setField(part, "partNumber", 1);
        ReflectionTestUtils.setField(part, "name", "정규화");
        ReflectionTestUtils.setField(part, "content", "정규화는 중복을 줄이는 과정입니다.");
        return part;
    }
}
