package com.f1.quiket.domain.quiz.service;

import com.f1.quiket.domain.part.entity.Part;
import com.f1.quiket.domain.quiz.dto.QuizAiGenerationPrompt;
import com.f1.quiket.domain.quiz.dto.QuizAiGenerationRequest;
import java.util.Comparator;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class QuizGenerationPromptBuilder {

    public QuizAiGenerationPrompt build(QuizAiGenerationRequest request) {
        return new QuizAiGenerationPrompt(systemMessage(), userMessage(request));
    }

    private String systemMessage() {
        return """
                너는 Quiket의 학습 퀴즈 생성 엔진이다.
                반드시 제공된 JSON Schema에 맞는 JSON만 반환한다.
                자연어 설명, 마크다운, 코드블록은 절대 반환하지 않는다.
                모든 문항은 제공된 partId 중 하나에만 종속되어야 한다.
                """;
    }

    private String userMessage(QuizAiGenerationRequest request) {
        return """
                [과목 정보]
                - 과목명: %s
                - 학습 목적: %s

                [과목 상세 정보]
                %s

                [퀴즈 설정]
                - 문제 유형: %s
                - 객관식 보기 수: %s
                - 문제 수: %d
                - 풀이 방식: %s
                - 타이머 사용 여부: %s
                - 타이머 범위: %s
                - 타이머 초: %s
                - 난이도: %s

                [난이도 출제 기준]
                %s

                [출제 범위]
                %s

                [생성 규칙]
                - questions 배열 길이는 요청 문제 수와 정확히 같아야 한다.
                - questionType은 요청한 문제 유형과 같아야 한다.
                - partId는 출제 범위에 제공된 partId 중 하나만 사용한다.
                - summary는 8자 이상 20자 이하의 한국어 핵심 요약이다.
                - body는 문제 본문이다.
                - correctExplanation과 incorrectExplanation은 각각 5자 이상 작성한다.
                - multiple_choice는 options를 요청 보기 수만큼 만들고 answerValue는 정답 optionNumber 문자열이다.
                - ox는 options를 빈 배열로 두고 answerValue는 O 또는 X만 사용한다.
                - 문제 본문에 정답을 직접 노출하지 않는다.
                - 정답 선택지는 질문의 핵심 표현을 어미만 바꾸어 반복하지 않고 구체적인 개념, 원인 또는 결과를 제시한다.
                - 오답 선택지도 학습 자료의 맥락에서 혼동할 수 있는 내용으로 구성하고 명백히 무관한 표현은 사용하지 않는다.
                - 선택지끼리 의미가 중복되지 않으며 동일한 범주와 문법 형태로 구성한다.
                - '위의 모든 것', '정답 없음', '알 수 없음'과 같은 포괄적 선택지는 사용하지 않는다.
                - 동일하거나 문장 부호와 공백만 다른 문항 및 선택지를 생성하지 않는다.
                - 응답 전 모든 문항과 선택지가 위 품질 규칙을 충족하는지 자체 검수한다.
                """.formatted(
                request.subject().getName(),
                request.subject().getPurpose(),
                subjectMetadata(request),
                request.quizType(),
                valueOrNone(request.choiceCount()),
                request.questionCount(),
                request.playMode(),
                Boolean.TRUE.equals(request.timerEnabled()),
                valueOrNone(request.timerScope()),
                valueOrNone(request.timerSeconds()),
                request.difficulty(),
                difficultyGuideline(request.difficulty()),
                partContext(request)
        );
    }

    private String difficultyGuideline(String difficulty) {
        String profile = switch (difficulty) {
            case "easy" -> """
                    - 표시 난이도: 쉬움
                    - 문항 구성: 핵심 개념 80% / 기본 응용 20%
                    - 핵심 개념: 자료에 명시된 정의, 원리, 핵심 용어를 직접 확인한다.
                    - 기본 응용: 한 단계 추론으로 배운 개념을 익숙한 상황에 적용한다.
                    - 복합 추론이나 지엽적인 함정보다 핵심 이해 확인을 우선한다.
                    """;
            case "medium" -> """
                    - 표시 난이도: 보통
                    - 문항 구성: 핵심 개념 40% / 지엽 사실 40% / 기본 응용 20%
                    - 핵심 개념: 자료의 주요 정의와 원리를 확인한다.
                    - 지엽 사실: 자료에 명시된 세부 조건, 특징, 비교 요소를 확인한다.
                    - 기본 응용: 한 단계 추론으로 배운 개념을 상황에 적용한다.
                    """;
            case "hard" -> """
                    - 표시 난이도: 어려움
                    - 문항 구성: 지엽 사실 30% / 개념 응용 50% / 복합 사고 20%
                    - 지엽 사실: 자료의 세부 조건과 예외를 구분한다.
                    - 개념 응용: 학습한 개념을 새로운 사례나 조건에 적용한다.
                    - 복합 사고: 둘 이상의 개념과 근거를 연결해 결론을 도출한다.
                    - 문장만 어렵게 바꾸지 말고 실제 추론 단계와 적용 깊이를 높인다.
                    """;
            default -> throw new IllegalArgumentException("지원하지 않는 퀴즈 난이도입니다.");
        };
        return """
                %s
                - 문제 수가 적어 정확한 비율 분배가 불가능하면 비중이 큰 출제 유형을 우선한다.
                - 모든 문항의 difficulty 필드값: %s
                """.formatted(profile.strip(), difficulty).strip();
    }

    private String subjectMetadata(QuizAiGenerationRequest request) {
        if (request.subjectMetadata() == null || request.subjectMetadata().isEmpty()) {
            return "- 없음";
        }
        return request.subjectMetadata().entrySet().stream()
                .map(entry -> "- %s: %s".formatted(entry.getKey(), entry.getValue()))
                .collect(Collectors.joining("\n"));
    }

    private String partContext(QuizAiGenerationRequest request) {
        return request.parts().stream()
                .sorted(Comparator.comparing(Part::getChapterId).thenComparing(Part::getPartNumber))
                .map(part -> """
                        - partId: %s
                          chapterId: %d
                          partName: %s
                          content: %s
                        """.formatted(
                        part.getPublicId(),
                        part.getChapterId(),
                        part.getName(),
                        valueOrNone(part.getContent())
                ))
                .collect(Collectors.joining("\n"));
    }

    private String valueOrNone(Object value) {
        return value == null ? "없음" : value.toString();
    }
}
