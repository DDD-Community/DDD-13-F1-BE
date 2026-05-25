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

                [퀴즈 설정]
                - 문제 유형: %s
                - 객관식 보기 수: %s
                - 문제 수: %d
                - 풀이 방식: %s
                - 타이머 사용 여부: %s
                - 타이머 범위: %s
                - 타이머 초: %s
                - 난이도: %s

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
                """.formatted(
                request.subject().getName(),
                request.subject().getPurpose(),
                request.quizType(),
                valueOrNone(request.choiceCount()),
                request.questionCount(),
                request.playMode(),
                Boolean.TRUE.equals(request.timerEnabled()),
                valueOrNone(request.timerScope()),
                valueOrNone(request.timerSeconds()),
                request.difficulty(),
                partContext(request)
        );
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
